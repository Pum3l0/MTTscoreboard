package io.github.pum3l0.scoreboard.ui.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.pum3l0.scoreboard.data.match.MatchStore
import io.github.pum3l0.scoreboard.data.match.MatchTiming
import io.github.pum3l0.scoreboard.data.match.StoredMatch
import io.github.pum3l0.scoreboard.domain.match.MatchAction
import io.github.pum3l0.scoreboard.domain.match.MatchConfig
import io.github.pum3l0.scoreboard.domain.match.MatchFormat
import io.github.pum3l0.scoreboard.domain.match.MatchReducer
import io.github.pum3l0.scoreboard.domain.match.MatchState
import io.github.pum3l0.scoreboard.domain.match.PlayerSide
import io.github.pum3l0.scoreboard.domain.match.Score
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface MatchUiState {
    data object Loading : MatchUiState
    data object LoadFailed : MatchUiState
    data class Home(val pendingMatch: MatchState? = null, val sidesSwapped: Boolean = false) : MatchUiState
    data object Settings : MatchUiState
    data class Rules(val fromSettings: Boolean) : MatchUiState
    data class History(val matches: List<StoredMatch>? = null, val failed: Boolean = false) : MatchUiState
    data class Archived(val match: StoredMatch, val history: History) : MatchUiState

    data class Setup(
        val firstName: String = "",
        val secondName: String = "",
        val format: MatchFormat = MatchFormat.BEST_OF_THREE,
    ) : MatchUiState {
        val canStart: Boolean
            get() = firstName.isNotBlank() && secondName.isNotBlank()
    }

    data class ChooseServer(val setup: Setup) : MatchUiState

    data class Session(
        val match: MatchState,
        val sidesSwapped: Boolean = false,
        val resultTiming: MatchTiming? = null,
    ) : MatchUiState
}

sealed interface HistoryTransfer {
    data object Working : HistoryTransfer
    data object Exported : HistoryTransfer
    data object Cleared : HistoryTransfer
    data class Imported(val count: Int) : HistoryTransfer
    data object Failed : HistoryTransfer
}

data class MatchClock(val elapsedMillis: Long? = null, val timeoutMillis: Long? = null)

class MatchViewModel(
    private val store: MatchStore,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val monotonicMillis: () -> Long = { System.nanoTime() / 1_000_000L },
) : ViewModel(scope) {
    private val mutableState = MutableStateFlow<MatchUiState>(MatchUiState.Loading)
    val state = mutableState.asStateFlow()

    private val mutableStorageError = MutableStateFlow(false)
    val storageError = mutableStorageError.asStateFlow()

    private val mutableTransfer = MutableStateFlow<HistoryTransfer?>(null)
    val transfer = mutableTransfer.asStateFlow()

    private val mutableAnnouncements = MutableSharedFlow<MatchAnnouncement>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    internal val announcements = mutableAnnouncements.asSharedFlow()

    private val mutableClock = MutableStateFlow(MatchClock())
    val clock = mutableClock.asStateFlow()
    private val mutableTimeoutEnded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val timeoutEnded = mutableTimeoutEnded.asSharedFlow()
    private var foreground = false
    private var activeSince: Long? = null
    private var timeoutUntil: Long? = null
    private var ticker: Job? = null

    private val writes = Mutex()
    private var pending: StoredMatch? = null

    init {
        restore()
    }

    fun restore() = launchOperation(onFailure = { mutableState.value = MatchUiState.LoadFailed }) {
        mutableState.value = MatchUiState.Loading
        val restored = store.loadPending()
        pending = restored
        mutableState.value = homeState()
        syncClock()
    }

    fun openSetup() = launchOperation {
        if (mutableState.value is MatchUiState.Home) {
            mutableState.value = MatchUiState.Setup()
        }
    }

    fun resumeMatch() = launchOperation {
        if (mutableState.value is MatchUiState.Home) {
            pending?.let {
                showPending(it)
                announceServer(it.match)
            }
        }
    }

    fun openHistory() = launchOperation(onFailure = { mutableState.value = MatchUiState.History(failed = true) }) {
        mutableState.value = MatchUiState.History()
        mutableState.value = MatchUiState.History(store.loadHistory())
    }

    fun clearHistory() = launchOperation(onFailure = {
        mutableTransfer.value = null
        mutableStorageError.value = true
    }) {
        if (mutableState.value !is MatchUiState.History) return@launchOperation
        mutableTransfer.value = HistoryTransfer.Working
        store.clearHistory()
        mutableState.value = MatchUiState.History(emptyList())
        mutableTransfer.value = HistoryTransfer.Cleared
    }

    fun exportHistory(write: suspend (List<StoredMatch>) -> Unit) =
        launchOperation(onFailure = { mutableTransfer.value = HistoryTransfer.Failed }) {
            mutableTransfer.value = HistoryTransfer.Working
            write(store.loadHistory())
            mutableTransfer.value = HistoryTransfer.Exported
        }

    fun importHistory(read: suspend () -> List<StoredMatch>) =
        launchOperation(onFailure = { mutableTransfer.value = HistoryTransfer.Failed }) {
            mutableTransfer.value = HistoryTransfer.Working
            val count = store.importHistory(read())
            if (mutableState.value is MatchUiState.History) {
                mutableState.value = MatchUiState.History(store.loadHistory())
            }
            mutableTransfer.value = HistoryTransfer.Imported(count)
        }

    fun clearTransfer() {
        mutableTransfer.value = null
    }

    fun openArchived(match: StoredMatch) = launchOperation {
        val history = mutableState.value as? MatchUiState.History ?: return@launchOperation
        mutableState.value = MatchUiState.Archived(match, history)
    }

    fun returnToHistory() = launchOperation {
        val archived = mutableState.value as? MatchUiState.Archived ?: return@launchOperation
        mutableState.value = archived.history
    }

    fun homeState(): MatchUiState.Home = MatchUiState.Home(pending?.match, pending?.sidesSwapped == true)

    fun openSettings() = launchOperation { mutableState.value = MatchUiState.Settings }

    fun openRules() = launchOperation {
        val current = mutableState.value
        if (current is MatchUiState.Home || current == MatchUiState.Settings) {
            mutableState.value = MatchUiState.Rules(fromSettings = current == MatchUiState.Settings)
        }
    }

    fun closeRules() = launchOperation {
        val rules = mutableState.value as? MatchUiState.Rules ?: return@launchOperation
        mutableState.value = if (rules.fromSettings) MatchUiState.Settings else homeState()
    }

    fun setName(player: PlayerSide, name: String) = updateSetup {
        when (player) {
            PlayerSide.FIRST -> copy(firstName = name)
            PlayerSide.SECOND -> copy(secondName = name)
        }
    }

    fun setFormat(format: MatchFormat) = updateSetup { copy(format = format) }

    fun startMatch() = launchOperation {
        val current = mutableState.value as? MatchUiState.Setup ?: return@launchOperation
        if (current.canStart) mutableState.value = MatchUiState.ChooseServer(current)
    }

    fun editSetup() = launchOperation {
        val current = mutableState.value as? MatchUiState.ChooseServer ?: return@launchOperation
        mutableState.value = current.setup
    }

    fun chooseFirstServer(player: PlayerSide) = launchOperation {
        val current = (mutableState.value as? MatchUiState.ChooseServer)?.setup ?: return@launchOperation
        val config = MatchConfig(
            firstPlayerName = current.firstName.trim(),
            secondPlayerName = current.secondName.trim(),
            format = current.format,
            firstServer = player,
        )
        val match = StoredMatch(
            UUID.randomUUID().toString(), MatchReducer.start(config), System.currentTimeMillis(), timing = MatchTiming(),
        )
        store.create(match, replacingId = pending?.id)
        showPending(match)
        announceServer(match.match)
    }

    fun dispatch(action: MatchAction) = launchOperation {
        if (mutableState.value !is MatchUiState.Session) return@launchOperation
        val now = monotonicMillis()
        expireTimeout(now)
        if (action is MatchAction.AwardPoint && timeoutUntil != null) return@launchOperation
        val current = checkNotNull(pending)
        val next = MatchReducer.reduce(current.match, action)
        if (next != current.match) {
            val updated = current.copy(
                match = next,
                timing = sampledTiming(current, now)?.atGameCount(next.completedGames.size),
            )
            store.save(current, updated)
            if (activeSince != null) activeSince = now
            showPending(updated)
            val score = if (next.completedGames.size > current.match.completedGames.size) {
                next.completedGames.last().score
            } else {
                next.activeGameScore
            }
            val displayedScore = if (score != null && updated.sidesSwapped) Score(score.second, score.first) else score
            val newService = next.server != current.match.server ||
                next.completedGames.size != current.match.completedGames.size
            mutableAnnouncements.tryEmit(MatchAnnouncement(displayedScore, next.serverName().takeIf { newService }))
        }
    }

    private fun announceServer(match: MatchState) {
        match.serverName()?.let { mutableAnnouncements.tryEmit(MatchAnnouncement(serverName = it)) }
    }

    fun swapSides() = launchOperation {
        if (mutableState.value !is MatchUiState.Session) return@launchOperation
        val current = checkNotNull(pending)
        if (current.match.winner != null) return@launchOperation
        val next = current.copy(sidesSwapped = !current.sidesSwapped)
        store.save(current, next)
        showPending(next)
    }

    fun finishMatch() = launchOperation {
        if (mutableState.value !is MatchUiState.Session) return@launchOperation
        val current = checkNotNull(pending)
        if (current.match.winner == null) return@launchOperation
        store.save(current, current.copy(finishedAt = System.currentTimeMillis()))
        pending = null
        mutableState.value = MatchUiState.Home()
        syncClock()
    }

    fun discardMatch() = launchOperation {
        if (mutableState.value !is MatchUiState.Session) return@launchOperation
        store.discardPending(checkNotNull(pending).id)
        pending = null
        mutableState.value = MatchUiState.Home()
        syncClock()
    }

    fun goHome() = launchOperation {
        checkpointTiming(monotonicMillis())
        timeoutUntil = null
        mutableState.value = homeState()
        syncClock()
    }

    fun setForeground(visible: Boolean) = launchOperation {
        if (foreground == visible) return@launchOperation
        foreground = visible
        try {
            if (!visible) checkpointTiming(monotonicMillis())
        } finally {
            syncClock()
        }
    }

    fun startTimeout(seconds: Int) = launchOperation {
        val session = mutableState.value as? MatchUiState.Session ?: return@launchOperation
        if (session.match.winner != null || timeoutUntil != null || seconds !in 15..180) return@launchOperation
        val now = monotonicMillis()
        checkpointTiming(now)
        timeoutUntil = now + seconds * 1_000L
        syncClock()
    }

    fun endTimeout() = launchOperation {
        timeoutUntil = null
        syncClock()
    }

    private fun sampledTiming(match: StoredMatch, now: Long): MatchTiming? {
        val timing = match.timing ?: return null
        val since = activeSince ?: return timing
        return timing.copy(activeGameMillis = timing.activeGameMillis + (now - since).coerceAtLeast(0))
    }

    private suspend fun checkpointTiming(now: Long) {
        val previous = pending ?: return
        val timing = sampledTiming(previous, now)
        if (timing != previous.timing) {
            val updated = previous.copy(timing = timing)
            store.save(previous, updated)
            pending = updated
        }
        if (activeSince != null) activeSince = now
    }

    private fun expireTimeout(now: Long) {
        val deadline = timeoutUntil ?: return
        if (now >= deadline) {
            timeoutUntil = null
            if (foreground) mutableTimeoutEnded.tryEmit(Unit)
            if (foreground && pending?.timing != null) activeSince = now
        }
    }

    private fun syncClock() {
        val current = mutableState.value
        val live = current is MatchUiState.Session && current.match.winner == null
        val now = monotonicMillis()
        if (live && foreground) expireTimeout(now)
        if (!live) timeoutUntil = null
        if (live && foreground && timeoutUntil == null && pending?.timing != null) {
            if (activeSince == null) activeSince = now
        } else {
            activeSince = null
        }
        mutableClock.value = if (live) {
            MatchClock(pending?.let { sampledTiming(it, now)?.totalMillis }, timeoutUntil?.let { (it - now).coerceAtLeast(0L) })
        } else {
            MatchClock()
        }
        if (live && foreground && (activeSince != null || timeoutUntil != null)) {
            if (ticker?.isActive != true) ticker = viewModelScope.launch {
                while (isActive) {
                    delay(1_000)
                    writes.withLock {
                        val tick = monotonicMillis()
                        try {
                            if (activeSince?.let { tick - it >= 5_000 } == true) checkpointTiming(tick)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            mutableStorageError.value = true
                        }
                        syncClock()
                    }
                }
            }
        } else {
            ticker?.cancel()
            ticker = null
        }
    }

    fun clearStorageError() {
        mutableStorageError.value = false
    }

    private fun showPending(match: StoredMatch) {
        pending = match
        mutableState.value = MatchUiState.Session(
            match.match, match.sidesSwapped, resultTiming = match.timing.takeIf { match.match.winner != null },
        )
        syncClock()
    }

    private fun updateSetup(update: MatchUiState.Setup.() -> MatchUiState.Setup) {
        mutableState.update { current ->
            if (current is MatchUiState.Setup) current.update() else current
        }
    }

    private fun launchOperation(
        onFailure: () -> Unit = { mutableStorageError.value = true },
        operation: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            writes.withLock {
                try {
                    operation()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    onFailure()
                }
            }
        }
    }
}

internal data class MatchAnnouncement(val score: Score? = null, val serverName: String? = null)

private fun MatchState.serverName(): String? = when (server) {
    PlayerSide.FIRST -> config.firstPlayerName
    PlayerSide.SECOND -> config.secondPlayerName
    null -> null
}

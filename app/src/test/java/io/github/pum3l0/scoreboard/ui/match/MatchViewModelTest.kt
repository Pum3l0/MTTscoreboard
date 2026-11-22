package io.github.pum3l0.scoreboard.ui.match

import io.github.pum3l0.scoreboard.data.match.MatchStore
import io.github.pum3l0.scoreboard.data.match.StoredMatch
import io.github.pum3l0.scoreboard.domain.match.MatchAction
import io.github.pum3l0.scoreboard.domain.match.MatchFormat
import io.github.pum3l0.scoreboard.domain.match.PlayerSide
import io.github.pum3l0.scoreboard.domain.match.Score
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchViewModelTest {
    @Test
    fun setupRequiresNamesAndStartsWithSelectedFormatAndServer() {
        val viewModel = viewModel(MemoryMatchStore())
        viewModel.openSetup()
        viewModel.setName(PlayerSide.FIRST, " Анна ")
        viewModel.setName(PlayerSide.SECOND, "  ")
        viewModel.startMatch()
        assertTrue(viewModel.state.value is MatchUiState.Setup)

        viewModel.setName(PlayerSide.SECOND, " Борис ")
        viewModel.setFormat(MatchFormat.BEST_OF_FIVE)
        viewModel.startMatch()
        assertTrue(viewModel.state.value is MatchUiState.ChooseServer)
        viewModel.chooseFirstServer(PlayerSide.SECOND)

        val match = (viewModel.state.value as MatchUiState.Session).match
        assertEquals("Анна", match.config.firstPlayerName)
        assertEquals("Борис", match.config.secondPlayerName)
        assertEquals(MatchFormat.BEST_OF_FIVE, match.config.format)
        assertEquals(PlayerSide.SECOND, match.server)
    }

    @Test
    fun undoWinningPointReturnsToActiveMatch() {
        val viewModel = viewModel(MemoryMatchStore())
        viewModel.openSetup()
        viewModel.setName(PlayerSide.FIRST, "Анна")
        viewModel.setName(PlayerSide.SECOND, "Борис")
        viewModel.setFormat(MatchFormat.BEST_OF_ONE)
        viewModel.startMatch()
        viewModel.chooseFirstServer(PlayerSide.FIRST)
        repeat(11) { viewModel.dispatch(MatchAction.AwardPoint(PlayerSide.FIRST)) }
        assertEquals(PlayerSide.FIRST, (viewModel.state.value as MatchUiState.Session).match.winner)

        viewModel.dispatch(MatchAction.UndoLastPoint)
        val activeMatch = (viewModel.state.value as MatchUiState.Session).match
        assertNull(activeMatch.winner)
        assertEquals(Score(10, 0), activeMatch.activeGameScore)

        viewModel.goHome()
        assertEquals(MatchUiState.Home(activeMatch), viewModel.state.value)
    }

    @Test
    fun pausedMatchAndUndoSurviveARecreatedViewModel() {
        val store = MemoryMatchStore()
        val original = viewModel(store)
        startOneGameMatch(original)
        repeat(3) { original.dispatch(MatchAction.AwardPoint(PlayerSide.SECOND)) }
        val previous = original.currentMatch()
        original.goHome()
        assertEquals(MatchUiState.Home(previous), original.state.value)

        val restored = viewModel(store)
        assertEquals(previous, (restored.state.value as MatchUiState.Home).pendingMatch)
        restored.resumeMatch()
        assertEquals(Score(0, 3), restored.currentMatch().activeGameScore)
        restored.dispatch(MatchAction.UndoLastPoint)
        assertEquals(Score(0, 2), (viewModel(store).state.value as MatchUiState.Home).pendingMatch?.activeGameScore)
    }

    @Test
    fun confirmedResultMovesToHistoryExactlyOnce() {
        val store = MemoryMatchStore()
        val original = viewModel(store)
        startOneGameMatch(original)
        repeat(11) { original.dispatch(MatchAction.AwardPoint(PlayerSide.FIRST)) }

        val restored = viewModel(store)
        restored.resumeMatch()
        assertEquals(PlayerSide.FIRST, restored.currentMatch().winner)
        assertTrue(store.history.isEmpty())
        restored.finishMatch()
        restored.finishMatch()
        assertNull(store.pending)
        assertEquals(1, store.history.size)
        restored.openHistory()
        assertEquals(1, (restored.state.value as MatchUiState.History).matches?.size)
    }

    @Test
    fun failedWriteKeepsTheLastSavedScore() {
        val store = MemoryMatchStore()
        val viewModel = viewModel(store)
        startOneGameMatch(viewModel)
        store.failWrites = true
        viewModel.dispatch(MatchAction.AwardPoint(PlayerSide.FIRST))
        assertEquals(Score(), viewModel.currentMatch().activeGameScore)
        assertTrue(viewModel.storageError.value)
    }

    @Test
    fun discardedMatchDoesNotRestoreOrAppearInHistory() {
        val store = MemoryMatchStore()
        val original = viewModel(store)
        startOneGameMatch(original)
        original.discardMatch()
        assertEquals(MatchUiState.Home(), viewModel(store).state.value)
        assertTrue(store.history.isEmpty())
    }

    @Test
    fun newMatchReplacesThePausedOneOnlyAfterPickingTheFirstServer() {
        val store = MemoryMatchStore()
        val viewModel = viewModel(store)
        startOneGameMatch(viewModel)
        viewModel.dispatch(MatchAction.AwardPoint(PlayerSide.SECOND))
        viewModel.goHome()
        val previous = checkNotNull(store.pending)

        viewModel.openSetup()
        viewModel.setName(PlayerSide.FIRST, "Ира")
        viewModel.setName(PlayerSide.SECOND, "Лев")
        viewModel.startMatch()
        assertEquals(previous, store.pending)
        viewModel.goHome()
        assertEquals(MatchUiState.Home(previous.match), viewModel.state.value)

        viewModel.openSetup()
        viewModel.setName(PlayerSide.FIRST, "Ира")
        viewModel.setName(PlayerSide.SECOND, "Лев")
        viewModel.startMatch()
        viewModel.chooseFirstServer(PlayerSide.SECOND)

        val replacement = checkNotNull(store.pending)
        assertTrue(previous.id != replacement.id)
        assertEquals(Score(), replacement.match.activeGameScore)
        assertEquals("Ира", replacement.match.config.firstPlayerName)
        assertEquals(PlayerSide.SECOND, replacement.match.server)
        assertTrue(store.history.isEmpty())
    }

    @Test
    fun swappingSidesPersistsAndDoesNotChangeTheMatch() {
        val store = MemoryMatchStore()
        val original = viewModel(store)
        startOneGameMatch(original)
        original.dispatch(MatchAction.AwardPoint(PlayerSide.FIRST))
        val beforeSwap = original.currentMatch()

        original.swapSides()
        assertEquals(MatchUiState.Session(beforeSwap, sidesSwapped = true), original.state.value)

        val restored = viewModel(store)
        assertEquals(MatchUiState.Home(beforeSwap, sidesSwapped = true), restored.state.value)
        restored.resumeMatch()
        restored.dispatch(MatchAction.UndoLastPoint)
        val session = restored.state.value as MatchUiState.Session
        assertTrue(session.sidesSwapped)
        assertEquals(Score(), session.match.activeGameScore)
        assertEquals(beforeSwap.config.firstServer, session.match.server)
    }

    @Test
    fun scoreAnnouncementKeepsTheCompletedGameScoreAndUsesDisplayOrder() {
        runBlocking {
            val viewModel = viewModel(MemoryMatchStore())
            viewModel.openSetup()
            viewModel.setName(PlayerSide.FIRST, "Анна")
            viewModel.setName(PlayerSide.SECOND, "Борис")
            viewModel.startMatch()
            viewModel.chooseFirstServer(PlayerSide.FIRST)
            repeat(10) { viewModel.dispatch(MatchAction.AwardPoint(PlayerSide.FIRST)) }
            viewModel.swapSides()

            val spoken = mutableListOf<MatchAnnouncement>()
            val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                viewModel.announcements.collect { spoken += it }
            }
            try {
                viewModel.dispatch(MatchAction.AwardPoint(PlayerSide.FIRST))
                yield()
                assertEquals(Score(), viewModel.currentMatch().activeGameScore)
                assertEquals(listOf(MatchAnnouncement(Score(0, 11), "Борис")), spoken)

                viewModel.dispatch(MatchAction.UndoLastPoint)
                yield()
                assertEquals(MatchAnnouncement(Score(0, 10), "Борис"), spoken.last())
            } finally {
                collector.cancel()
            }
        }
    }

    @Test
    fun announcesFirstServerAndServiceChangesOnly() = runBlocking {
        val viewModel = viewModel(MemoryMatchStore())
        val spoken = mutableListOf<MatchAnnouncement>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.announcements.collect { spoken += it }
        }
        try {
            startOneGameMatch(viewModel)
            yield()
            viewModel.dispatch(MatchAction.AwardPoint(PlayerSide.FIRST))
            yield()
            viewModel.dispatch(MatchAction.AwardPoint(PlayerSide.FIRST))
            yield()
            viewModel.goHome()
            viewModel.resumeMatch()
            yield()

            assertEquals(
                listOf(
                    MatchAnnouncement(serverName = "Анна"),
                    MatchAnnouncement(Score(1, 0)),
                    MatchAnnouncement(Score(2, 0), "Борис"),
                    MatchAnnouncement(serverName = "Борис"),
                ),
                spoken,
            )
        } finally {
            collector.cancel()
        }
    }

    @Test
    fun completedGameTimeExcludesTimeoutAndIsSavedInHistory() {
        val store = MemoryMatchStore()
        var now = 0L
        val viewModel = MatchViewModel(store, CoroutineScope(Dispatchers.Unconfined)) { now }
        try {
            startOneGameMatch(viewModel)
            viewModel.setForeground(true)
            now = 4_000
            viewModel.startTimeout(15)
            assertEquals(15_000L, viewModel.clock.value.timeoutMillis)
            now = 11_000
            viewModel.dispatch(MatchAction.AwardPoint(PlayerSide.FIRST))
            assertEquals(Score(), viewModel.currentMatch().activeGameScore)
            viewModel.endTimeout()

            now = 20_000
            repeat(11) { viewModel.dispatch(MatchAction.AwardPoint(PlayerSide.FIRST)) }
            val result = (viewModel.state.value as MatchUiState.Session).resultTiming
            assertEquals(listOf(13_000L), result?.gamesMillis)
            assertEquals(13_000L, result?.totalMillis)
            viewModel.finishMatch()
            assertEquals(result, store.history.values.single().timing)
        } finally {
            viewModel.setForeground(false)
        }
    }

    @Test
    fun timerPausesOnHomeAndUndoReopensThePreviousGameWithoutLosingTime() {
        val store = MemoryMatchStore()
        var now = 0L
        val viewModel = MatchViewModel(store, CoroutineScope(Dispatchers.Unconfined)) { now }
        try {
            viewModel.openSetup()
            viewModel.setName(PlayerSide.FIRST, "Анна")
            viewModel.setName(PlayerSide.SECOND, "Борис")
            viewModel.startMatch()
            viewModel.chooseFirstServer(PlayerSide.FIRST)
            viewModel.setForeground(true)

            now = 5_000
            repeat(11) { viewModel.dispatch(MatchAction.AwardPoint(PlayerSide.FIRST)) }
            now = 7_000
            viewModel.goHome()
            assertEquals(7_000L, store.pending?.timing?.totalMillis)

            now = 50_000
            viewModel.resumeMatch()
            now = 53_000
            viewModel.dispatch(MatchAction.UndoLastPoint)
            assertEquals(10_000L, store.pending?.timing?.activeGameMillis)
            assertTrue(store.pending?.timing?.gamesMillis?.isEmpty() == true)
            assertEquals(Score(10, 0), viewModel.currentMatch().activeGameScore)
        } finally {
            viewModel.setForeground(false)
        }
    }

    private fun MatchViewModel.currentMatch() = (state.value as MatchUiState.Session).match

    private fun viewModel(store: MemoryMatchStore) =
        MatchViewModel(store, CoroutineScope(Dispatchers.Unconfined))

    private fun startOneGameMatch(viewModel: MatchViewModel) {
        viewModel.openSetup()
        viewModel.setName(PlayerSide.FIRST, "Анна")
        viewModel.setName(PlayerSide.SECOND, "Борис")
        viewModel.setFormat(MatchFormat.BEST_OF_ONE)
        viewModel.startMatch()
        viewModel.chooseFirstServer(PlayerSide.FIRST)
    }

    private class MemoryMatchStore : MatchStore {
        val history = mutableMapOf<String, StoredMatch>()
        var pending: StoredMatch? = null
        var failWrites = false

        override suspend fun loadPending() = pending

        override suspend fun loadHistory() = history.values.toList()

        override suspend fun clearHistory() {
            history.clear()
        }

        override suspend fun importHistory(matches: List<StoredMatch>): Int =
            matches.count { history.putIfAbsent(it.id, it) == null }

        override suspend fun create(match: StoredMatch, replacingId: String?) {
            check(!failWrites && pending?.id == replacingId)
            pending = match
        }

        override suspend fun save(previous: StoredMatch, match: StoredMatch) {
            check(!failWrites && pending == previous)
            if (match.finishedAt == null) {
                pending = match
            } else {
                history[match.id] = match
                pending = null
            }
        }

        override suspend fun discardPending(id: String) {
            if (pending?.id == id) pending = null
        }
    }
}

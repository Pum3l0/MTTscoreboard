package io.github.pum3l0.scoreboard.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.github.pum3l0.scoreboard.R
import io.github.pum3l0.scoreboard.data.match.HistoryBackup
import io.github.pum3l0.scoreboard.data.settings.AppLanguage
import io.github.pum3l0.scoreboard.data.settings.MatchSpeech
import io.github.pum3l0.scoreboard.data.settings.ScoreFont
import io.github.pum3l0.scoreboard.data.settings.ScoreboardHaptics
import io.github.pum3l0.scoreboard.data.settings.ThemeMode
import io.github.pum3l0.scoreboard.data.settings.ThemePreferences
import io.github.pum3l0.scoreboard.domain.match.MatchAction
import io.github.pum3l0.scoreboard.domain.match.Score
import io.github.pum3l0.scoreboard.ui.components.AccentButton
import io.github.pum3l0.scoreboard.ui.components.PredictiveBackFrame
import io.github.pum3l0.scoreboard.ui.history.HistoryScreen
import io.github.pum3l0.scoreboard.ui.home.HomeScreen
import io.github.pum3l0.scoreboard.ui.match.ChooseFirstServerScreen
import io.github.pum3l0.scoreboard.ui.match.HistoryTransfer
import io.github.pum3l0.scoreboard.ui.match.MatchUiState
import io.github.pum3l0.scoreboard.ui.match.MatchViewModel
import io.github.pum3l0.scoreboard.ui.match.ScoreboardScreen
import io.github.pum3l0.scoreboard.ui.result.MatchResultScreen
import io.github.pum3l0.scoreboard.ui.rules.RulesScreen
import io.github.pum3l0.scoreboard.ui.settings.SettingsPage
import io.github.pum3l0.scoreboard.ui.settings.SettingsScreen
import io.github.pum3l0.scoreboard.ui.setup.MatchSetupScreen
import io.github.pum3l0.scoreboard.ui.theme.ScoreBoardTheme
import io.github.pum3l0.scoreboard.ui.theme.scoreFontFamily
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Destination(val label: Int, val icon: Int) {
    Home(R.string.home_tab, R.drawable.ic_home),
    History(R.string.history_tab, R.drawable.ic_history),
    Settings(R.string.settings, R.drawable.ic_settings),
}

@Composable
fun ScoreBoardApp(
    viewModel: MatchViewModel,
    theme: ThemePreferences,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDarkThemeChanged: (Boolean) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state
    val settings by theme.settings.collectAsStateWithLifecycle()
    val dark = when (settings.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    SideEffect { onDarkThemeChanged(dark) }

    val destination = current.destination()
    var settingsPage by rememberSaveable { mutableStateOf(SettingsPage.OVERVIEW) }
    val select: (Destination) -> Unit = {
        if (it != destination) {
            settingsPage = SettingsPage.OVERVIEW
            when (it) {
                Destination.Home -> viewModel.goHome()
                Destination.History -> viewModel.openHistory()
                Destination.Settings -> viewModel.openSettings()
            }
        }
    }

    val matchInProgress = current is MatchUiState.Session && current.match.winner == null
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var confirmReplace by rememberSaveable { mutableStateOf(false) }
    var confirmClearHistory by rememberSaveable { mutableStateOf(false) }
    val exit: () -> Unit = {
        when {
            current == MatchUiState.Settings && settingsPage != SettingsPage.OVERVIEW ->
                settingsPage = SettingsPage.OVERVIEW
            matchInProgress -> viewModel.goHome()
            current is MatchUiState.ChooseServer -> viewModel.editSetup()
            current is MatchUiState.Archived -> viewModel.returnToHistory()
            current is MatchUiState.Rules -> viewModel.closeRules()
            current is MatchUiState.Session -> viewModel.finishMatch()
            else -> viewModel.goHome()
        }
    }
    val backPreview = when (current) {
        is MatchUiState.Rules -> if (current.fromSettings) MatchUiState.Settings else viewModel.homeState()
        is MatchUiState.ChooseServer -> current.setup
        is MatchUiState.Archived -> current.history
        is MatchUiState.Session -> if (matchInProgress) viewModel.homeState() else MatchUiState.Home()
        MatchUiState.Settings -> if (settingsPage == SettingsPage.OVERVIEW) viewModel.homeState() else current
        is MatchUiState.Setup, is MatchUiState.History -> viewModel.homeState()
        else -> null
    }

    ScoreBoardTheme(darkTheme = dark, dynamicColor = settings.dynamicColor, appFont = settings.appFont, appAccent = settings.appAccent) {
        val language = settings.language
        if (language == null) {
            WelcomeScreen(onLanguageSelected)
            return@ScoreBoardTheme
        }
        val snackbarHost = remember { SnackbarHostState() }
        val storageError by viewModel.storageError.collectAsStateWithLifecycle()
        val storageErrorMessage = stringResource(R.string.storage_error)
        LaunchedEffect(storageError) {
            if (storageError) {
                snackbarHost.showSnackbar(storageErrorMessage)
                viewModel.clearStorageError()
            }
        }

        val context = LocalContext.current
        val resolver = context.contentResolver
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(lifecycle, viewModel) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.setForeground(true)
                try {
                    awaitCancellation()
                } finally {
                    viewModel.setForeground(false)
                }
            }
        }
        LaunchedEffect(lifecycle, viewModel, settings.timeoutSignal, settings.timeoutEnabled) {
            if (settings.timeoutEnabled && settings.timeoutSignal) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.timeoutEnded.collect {
                        val tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 45) }.getOrNull()
                        if (tone != null) {
                            try {
                                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 180)
                                delay(210)
                            } finally {
                                tone.release()
                            }
                        }
                    }
                }
            }
        }
        val speech = remember(context, language) { MatchSpeech(context.applicationContext, language) }
        val speechStatus by speech.status.collectAsStateWithLifecycle()
        SideEffect { speech.ratePercent = settings.speechRate }
        LaunchedEffect(lifecycle, speech, settings.announceScore, settings.announceServer, settings.scoreVoice) {
            if (settings.announceScore || settings.announceServer) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    speech.start(settings.scoreVoice)
                    try {
                        viewModel.announcements.collect { announcement ->
                            speech.speak(
                                announcement.score.takeIf { settings.announceScore },
                                announcement.serverName.takeIf { settings.announceServer },
                            )
                        }
                    } finally {
                        speech.stop()
                    }
                }
            }
        }
        val scope = rememberCoroutineScope()
        val haptics = remember(context) { ScoreboardHaptics(context) }
        val tap = { haptics.pulse(settings.hapticLevel) }
        val customFont = remember(theme, settings.customFontFile) { theme.customFont(settings.customFontFile) }
        val font = scoreFontFamily(settings.scoreFont, customFont)
        val fontError = stringResource(R.string.font_failed)
        val openFont = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    try {
                        theme.importFont(resolver, uri)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        snackbarHost.showSnackbar(fontError)
                    }
                }
            }
        }
        val openHistory = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) viewModel.importHistory { HistoryBackup.read(resolver, uri) }
        }
        val saveHistory = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) viewModel.exportHistory { HistoryBackup.write(resolver, uri, it) }
        }
        val transfer by viewModel.transfer.collectAsStateWithLifecycle()
        val transferMessage = when (val outcome = transfer) {
            HistoryTransfer.Exported -> stringResource(R.string.history_exported)
            HistoryTransfer.Cleared -> stringResource(R.string.history_cleared)
            is HistoryTransfer.Imported -> stringResource(R.string.history_imported, outcome.count)
            HistoryTransfer.Failed -> stringResource(R.string.history_transfer_failed)
            HistoryTransfer.Working, null -> null
        }
        LaunchedEffect(transferMessage) {
            if (transferMessage != null) {
                snackbarHost.showSnackbar(transferMessage)
                viewModel.clearTransfer()
            }
        }
        val motion = MaterialTheme.motionScheme

        val render: @Composable (MatchUiState, SettingsPage) -> Unit = { screen, page ->
            when (screen) {
                MatchUiState.Loading -> RestoreStatus(false, viewModel::restore, Modifier)
                MatchUiState.LoadFailed -> RestoreStatus(true, viewModel::restore, Modifier)
                is MatchUiState.Home -> HomeScreen(
                    onNewMatch = {
                        if (screen.pendingMatch != null) confirmReplace = true else viewModel.openSetup()
                    },
                    onResumeMatch = viewModel::resumeMatch,
                    onRules = viewModel::openRules,
                    pendingMatch = screen.pendingMatch,
                    sidesSwapped = screen.sidesSwapped,
                    scoreFamily = font,
                )
                is MatchUiState.History -> HistoryScreen(
                    state = screen,
                    onSelect = viewModel::openArchived,
                    onRetry = viewModel::openHistory,
                    onExport = { saveHistory.launch("scoreboard-history.json") },
                    onImport = { openHistory.launch(arrayOf("*/*")) },
                    onClear = { confirmClearHistory = true },
                    isTransferring = transfer == HistoryTransfer.Working,
                )
                MatchUiState.Settings -> SettingsScreen(
                    page = page,
                    settings = settings,
                    onNavigate = { settingsPage = it },
                    scoreFamily = font,
                    onChange = theme::save,
                    onLanguageSelected = onLanguageSelected,
                    onPickFont = { openFont.launch(arrayOf("*/*")) },
                    findFontFile = theme::customFont,
                    onTestHaptics = tap,
                    onOpenRules = viewModel::openRules,
                    speechStatus = speechStatus,
                    onTestSpeech = {
                        speech.speak(
                            Score(1, 3).takeIf { settings.announceScore },
                            context.getString(R.string.voice_demo_player).takeIf { settings.announceServer },
                        )
                    },
                )
                is MatchUiState.Rules -> RulesScreen(onBack = viewModel::closeRules)
                is MatchUiState.Setup -> MatchSetupScreen(
                    state = screen,
                    onNameChanged = viewModel::setName,
                    onFormatChanged = viewModel::setFormat,
                    onStart = viewModel::startMatch,
                    onBack = exit,
                )
                is MatchUiState.ChooseServer -> ChooseFirstServerScreen(
                    setup = screen.setup,
                    appearance = settings.board,
                    onChoose = {
                        tap()
                        viewModel.chooseFirstServer(it)
                    },
                    onBack = viewModel::editSetup,
                )
                is MatchUiState.Session -> {
                    val undo = {
                        tap()
                        viewModel.dispatch(MatchAction.UndoLastPoint)
                    }
                    if (screen.match.winner == null) {
                        val clock by viewModel.clock.collectAsStateWithLifecycle()
                        ScoreboardScreen(
                            state = screen.match,
                            fontFamily = font,
                            appearance = settings.board,
                            sevenSegment = settings.scoreFont == ScoreFont.SEVEN_SEGMENT,
                            sidesSwapped = screen.sidesSwapped,
                            clock = clock,
                            timeoutEnabled = settings.timeoutEnabled,
                            onTimeout = {
                                if (viewModel.clock.value.timeoutMillis != null) viewModel.endTimeout()
                                else viewModel.startTimeout(settings.timeoutSeconds)
                            },
                            onSwap = {
                                tap()
                                viewModel.swapSides()
                            },
                            onPoint = {
                                tap()
                                viewModel.dispatch(MatchAction.AwardPoint(it))
                            },
                            onUndo = undo,
                            onExit = { confirmExit = true },
                        )
                    } else {
                        MatchResultScreen(screen.match, viewModel::finishMatch, onUndo = undo, timing = screen.resultTiming, fontFamily = font)
                    }
                }
                is MatchUiState.Archived -> MatchResultScreen(
                    screen.match.match, viewModel::returnToHistory, timing = screen.match.timing, fontFamily = font,
                )
            }
        }
        PredictiveBackFrame(
            enabled = backPreview != null && !confirmExit && !confirmDiscard && !confirmReplace && !confirmClearHistory,
            screenKey = current::class to if (current == MatchUiState.Settings) settingsPage else null,
            onBack = exit,
            preview = {
                if (backPreview != null) {
                    ScreenChrome(backPreview.destination(), SettingsPage.OVERVIEW, null, select) {
                        render(backPreview, SettingsPage.OVERVIEW)
                    }
                }
            },
        ) {
            ScreenChrome(destination, settingsPage, snackbarHost, select) {
                AnimatedContent(
                    targetState = current,
                    modifier = Modifier.fillMaxSize(),
                    contentKey = { if (it is MatchUiState.Session) it.match.winner != null else it::class },
                    transitionSpec = {
                        val from = initialState.destination()
                        val to = targetState.destination()
                        if (from != null && to != null && from != to) {
                            val direction = if (to.ordinal > from.ordinal) 1 else -1
                            (slideInHorizontally(motion.fastSpatialSpec()) { direction * it / 8 } +
                                fadeIn(motion.fastEffectsSpec())) togetherWith
                                (slideOutHorizontally(motion.fastSpatialSpec()) { -direction * it / 8 } +
                                    fadeOut(motion.fastEffectsSpec()))
                        } else {
                            (fadeIn(motion.fastEffectsSpec()) +
                                scaleIn(motion.fastSpatialSpec(), initialScale = 0.98f)) togetherWith
                                fadeOut(motion.fastEffectsSpec())
                        }
                    },
                    label = "screen change",
                ) { screen -> render(screen, settingsPage) }
            }
        }

        if (confirmClearHistory && current is MatchUiState.History) {
            AlertDialog(
                onDismissRequest = { confirmClearHistory = false },
                title = { Text(stringResource(R.string.clear_history_title)) },
                text = { Text(stringResource(R.string.clear_history_message)) },
                confirmButton = {
                    TextButton(
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            confirmClearHistory = false
                            viewModel.clearHistory()
                        },
                    ) { Text(stringResource(R.string.clear_history)) }
                },
                dismissButton = {
                    TextButton(shapes = ButtonDefaults.shapes(), onClick = { confirmClearHistory = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
        if (confirmReplace && current is MatchUiState.Home && current.pendingMatch != null) {
            AlertDialog(
                onDismissRequest = { confirmReplace = false },
                title = { Text(stringResource(R.string.replace_match_title)) },
                text = { Text(stringResource(R.string.replace_match_message)) },
                confirmButton = {
                    TextButton(shapes = ButtonDefaults.shapes(), onClick = {
                        confirmReplace = false
                        viewModel.openSetup()
                    }) { Text(stringResource(R.string.new_match)) }
                },
                dismissButton = {
                    TextButton(shapes = ButtonDefaults.shapes(), onClick = { confirmReplace = false }) { Text(stringResource(R.string.cancel)) }
                },
            )
        }
        if (confirmExit && matchInProgress) {
            AlertDialog(
                onDismissRequest = { confirmExit = false },
                title = { Text(stringResource(R.string.exit_match_title)) },
                text = { Text(stringResource(R.string.exit_match_message)) },
                confirmButton = {
                    TextButton(shapes = ButtonDefaults.shapes(), onClick = {
                        confirmExit = false
                        viewModel.goHome()
                    }) { Text(stringResource(R.string.to_home)) }
                },
                dismissButton = {
                    TextButton(shapes = ButtonDefaults.shapes(), onClick = {
                        confirmExit = false
                        confirmDiscard = true
                    }) { Text(stringResource(R.string.discard_match)) }
                },
            )
        }
        if (confirmDiscard && matchInProgress) {
            AlertDialog(
                onDismissRequest = { confirmDiscard = false },
                title = { Text(stringResource(R.string.discard_match_title)) },
                text = { Text(stringResource(R.string.discard_match_message)) },
                confirmButton = {
                    TextButton(shapes = ButtonDefaults.shapes(), onClick = {
                        confirmDiscard = false
                        viewModel.discardMatch()
                    }) { Text(stringResource(R.string.discard_match)) }
                },
                dismissButton = {
                    TextButton(shapes = ButtonDefaults.shapes(), onClick = { confirmDiscard = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
    }
}

private fun MatchUiState.destination(): Destination? = when (this) {
    is MatchUiState.Home -> Destination.Home
    is MatchUiState.History -> Destination.History
    MatchUiState.Settings -> Destination.Settings
    else -> null
}

@Composable
private fun ScreenChrome(
    destination: Destination?,
    settingsPage: SettingsPage,
    snackbarHost: SnackbarHostState?,
    select: (Destination) -> Unit,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = destination != null && maxWidth >= 600.dp
        Row(Modifier.fillMaxSize()) {
            if (useRail) ScoreBoardRail(checkNotNull(destination), select)
            Scaffold(
                modifier = Modifier.weight(1f),
                snackbarHost = { if (snackbarHost != null) SnackbarHost(snackbarHost) },
                bottomBar = {
                    if (!useRail && destination != null &&
                        (destination != Destination.Settings || settingsPage == SettingsPage.OVERVIEW)
                    ) ScoreBoardBottomBar(destination, select)
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) { content() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RestoreStatus(failed: Boolean, onRetry: () -> Unit, modifier: Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        if (failed) {
            Text(
                stringResource(R.string.restore_failed),
                style = MaterialTheme.typography.titleMediumEmphasized,
                textAlign = TextAlign.Center,
            )
            AccentButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        } else {
            LoadingIndicator()
        }
    }
}

@Composable
private fun ScoreBoardBottomBar(destination: Destination, select: (Destination) -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.extraExtraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp).selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Destination.entries.forEach {
                    val interactions = remember { MutableInteractionSource() }
                    ShortNavigationBarItem(
                        modifier = Modifier.weight(1f),
                        selected = it == destination,
                        onClick = { select(it) },
                        icon = { NavigationGlyph(it, it == destination, interactions) },
                        interactionSource = interactions,
                        label = {
                            Text(stringResource(it.label), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreBoardRail(destination: Destination, select: (Destination) -> Unit) {
    WideNavigationRail(state = rememberWideNavigationRailState(WideNavigationRailValue.Collapsed)) {
        Destination.entries.forEach {
            val interactions = remember { MutableInteractionSource() }
            WideNavigationRailItem(
                selected = it == destination,
                onClick = { select(it) },
                icon = { NavigationGlyph(it, it == destination, interactions) },
                interactionSource = interactions,
                label = { Text(stringResource(it.label)) },
                railExpanded = false,
            )
        }
    }
}

@Composable
private fun NavigationGlyph(destination: Destination, selected: Boolean, interactions: MutableInteractionSource) {
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.86f else if (selected) 1.07f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "navigation icon press",
    )
    Icon(painterResource(destination.icon), contentDescription = null, modifier = Modifier.scale(scale))
}

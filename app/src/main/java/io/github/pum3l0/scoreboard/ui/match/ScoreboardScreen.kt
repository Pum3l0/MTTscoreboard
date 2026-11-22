package io.github.pum3l0.scoreboard.ui.match

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toPath
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import io.github.pum3l0.scoreboard.R
import io.github.pum3l0.scoreboard.data.settings.BoardAppearance
import io.github.pum3l0.scoreboard.data.settings.BoardLayoutMode
import io.github.pum3l0.scoreboard.data.settings.BoardPalette
import io.github.pum3l0.scoreboard.data.settings.ScoreScale
import io.github.pum3l0.scoreboard.domain.match.MatchState
import io.github.pum3l0.scoreboard.domain.match.PlayerSide
import io.github.pum3l0.scoreboard.ui.theme.contrastingText
import io.github.pum3l0.scoreboard.ui.theme.toneLike

@Composable
fun ChooseFirstServerScreen(
    setup: MatchUiState.Setup,
    onChoose: (PlayerSide) -> Unit,
    onBack: () -> Unit,
    appearance: BoardAppearance,
    modifier: Modifier = Modifier,
) {
    BoardLayout(
        title = stringResource(R.string.first_server),
        modifier = modifier,
        layout = appearance.layout,
        actions = { TextButton(onClick = onBack, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.back)) } },
    ) { side, panelModifier ->
        PlayerPanel(
            side = side,
            name = if (side == PlayerSide.FIRST) setup.firstName.trim() else setup.secondName.trim(),
            onClick = { onChoose(side) },
            modifier = panelModifier,
            appearance = appearance,
        )
    }
}

@Composable
fun ScoreboardScreen(
    state: MatchState,
    fontFamily: FontFamily,
    appearance: BoardAppearance,
    sevenSegment: Boolean,
    sidesSwapped: Boolean,
    onSwap: () -> Unit,
    clock: MatchClock,
    timeoutEnabled: Boolean,
    onTimeout: () -> Unit,
    onPoint: (PlayerSide) -> Unit,
    onUndo: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val score = checkNotNull(state.activeGameScore)
    val view = LocalView.current
    DisposableEffect(view, appearance.keepScreenOn) {
        val keepScreenOn = view.keepScreenOn
        if (appearance.keepScreenOn) view.keepScreenOn = true
        onDispose { view.keepScreenOn = keepScreenOn }
    }

    BoardLayout(
        title = stringResource(R.string.game_number, state.completedGames.size + 1),
        modifier = modifier,
        layout = appearance.layout,
        sidesSwapped = sidesSwapped,
        status = {
            val remaining = clock.timeoutMillis
            val time = clock.elapsedMillis
            if (remaining != null) {
                TimeChip(stringResource(R.string.timeout_remaining, elapsedTime(remaining + 999)), active = true)
            } else if (appearance.showElapsed && time != null) {
                TimeChip(elapsedTime(time), active = false)
            }
        },
        actions = {
            TextButton(onClick = onExit, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.exit_match)) }
            FilledTonalButton(onClick = onSwap, shapes = ButtonDefaults.shapes()) {
                Icon(
                    painterResource(R.drawable.ic_swap),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(stringResource(R.string.swap_sides_short), modifier = Modifier.padding(start = 6.dp))
            }
            if (timeoutEnabled) {
                FilledTonalButton(onClick = onTimeout, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(if (clock.timeoutMillis != null) R.string.end_timeout else R.string.timeout_title))
                }
            }
            FilledTonalButton(
                onClick = onUndo,
                enabled = state.canUndo,
                shapes = ButtonDefaults.shapes(),
            ) {
                Icon(painterResource(R.drawable.ic_history), contentDescription = null, Modifier.size(20.dp))
                Text(stringResource(R.string.undo_short), modifier = Modifier.padding(start = 6.dp))
            }
        },
    ) { side, panelModifier ->
        val first = side == PlayerSide.FIRST
        PlayerPanel(
            side = side,
            name = if (first) state.config.firstPlayerName else state.config.secondPlayerName,
            points = if (first) score.first else score.second,
            games = if (first) state.gamesWon.first else state.gamesWon.second,
            serving = state.server == side,
            fontFamily = fontFamily,
            sevenSegment = sevenSegment,
            onClick = { onPoint(side) },
            modifier = panelModifier,
            appearance = appearance,
        )
    }
}

@Composable
private fun BoardLayout(
    title: String,
    modifier: Modifier,
    layout: BoardLayoutMode,
    sidesSwapped: Boolean = false,
    status: @Composable () -> Unit = {},
    actions: @Composable () -> Unit,
    player: @Composable (PlayerSide, Modifier) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        val horizontal = useHorizontalLayout(layout, maxWidth >= 600.dp || maxWidth > maxHeight)
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                title,
                modifier = Modifier.fillMaxWidth().semantics { heading() },
                style = MaterialTheme.typography.titleLargeEmphasized,
                textAlign = TextAlign.Center,
            )
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { status() }
            val motion = MaterialTheme.motionScheme
            AnimatedContent(
                targetState = sidesSwapped,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn(motion.fastEffectsSpec()) + scaleIn(motion.fastSpatialSpec(), initialScale = 0.96f)) togetherWith
                        fadeOut(motion.fastEffectsSpec())
                },
                label = "swap player panels",
            ) { swapped ->
                PlayerPanels(horizontal, Modifier.fillMaxSize(), swapped, player)
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.Center,
            ) { actions() }
        }
    }
}

@Composable
private fun PlayerPanels(
    horizontal: Boolean,
    modifier: Modifier,
    swapped: Boolean = false,
    player: @Composable (PlayerSide, Modifier) -> Unit,
) {
    val first = if (swapped) PlayerSide.SECOND else PlayerSide.FIRST
    if (horizontal) {
        Row(modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            player(first, Modifier.weight(1f))
            player(first.opponent, Modifier.weight(1f))
        }
    } else {
        Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            player(first, Modifier.weight(1f))
            player(first.opponent, Modifier.weight(1f))
        }
    }
}

private fun useHorizontalLayout(layout: BoardLayoutMode, wide: Boolean): Boolean = when (layout) {
    BoardLayoutMode.AUTO -> wide
    BoardLayoutMode.SIDE_BY_SIDE -> true
    BoardLayoutMode.STACKED -> false
}

@Composable
fun ScoreboardPreview(
    appearance: BoardAppearance,
    fontFamily: FontFamily,
    sevenSegment: Boolean,
    modifier: Modifier = Modifier,
) {
    var first by rememberSaveable { mutableIntStateOf(8) }
    var second by rememberSaveable { mutableIntStateOf(10) }
    BoxWithConstraints(modifier) {
        val horizontal = useHorizontalLayout(appearance.layout, wide = true)
        val total = first + second
        val serviceTurns = if (first >= 10 && second >= 10) total - 20 else total / 2
        val servingSide = if (serviceTurns % 2 == 0) PlayerSide.FIRST else PlayerSide.SECOND
        PlayerPanels(horizontal, Modifier.fillMaxWidth().height(if (horizontal) 150.dp else 260.dp)) { side, panelModifier ->
            val isFirst = side == PlayerSide.FIRST
            PlayerPanel(
                side = side,
                name = stringResource(if (isFirst) R.string.player_one else R.string.player_two),
                onClick = {
                    if (isFirst) first = if (first >= 20) 8 else first + 1
                    else second = if (second >= 20) 10 else second + 1
                },
                modifier = panelModifier,
                points = if (isFirst) first else second,
                games = if (isFirst) 1 else 0,
                serving = side == servingSide,
                fontFamily = fontFamily,
                sevenSegment = sevenSegment,
                appearance = appearance,
                numberLimit = appearance.scale.previewLimit,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayerPanel(
    side: PlayerSide,
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    points: Int? = null,
    games: Int = 0,
    serving: Boolean = false,
    fontFamily: FontFamily = FontFamily.Default,
    sevenSegment: Boolean = false,
    appearance: BoardAppearance,
    numberLimit: TextUnit = appearance.scale.fontLimit,
) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val first = side == PlayerSide.FIRST
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    var shapeStep by rememberSaveable(side) { mutableIntStateOf(0) }
    val step = if (appearance.shiftingShapes) shapeStep else 0
    val active = serving || points == null
    val (baseBackground, baseForeground) = when (appearance.palette) {
        BoardPalette.TWO_TONE -> when {
            first -> colors.primaryContainer to colors.onPrimaryContainer
            else -> colors.tertiaryContainer to colors.onTertiaryContainer
        }.let { if (active) it else colors.surfaceContainerHigh to colors.onSurface }
        BoardPalette.THEME ->
            if (active) colors.primaryContainer to colors.onPrimaryContainer
            else colors.surfaceContainerHighest to colors.onSurface
        BoardPalette.HIGH_CONTRAST -> when {
            !active -> colors.surfaceContainerLow to colors.onSurface
            appearance.shiftingShapes -> colors.surfaceContainerHighest to colors.onSurface
            else -> colors.onSurface to colors.surface
        }
    }
    val accent = appearance.colors.forPlayer(appearance.palette, first)?.let { Color(it) }
    val backgroundTarget = remember(accent, baseBackground, active) {
        accent?.toneLike(baseBackground, saturationScale = if (active) 1f else 0.42f) ?: baseBackground
    }
    val foregroundTarget = backgroundTarget.contrastingText(baseForeground)
    val badgeBase = when {
        appearance.palette == BoardPalette.HIGH_CONTRAST ->
            if (appearance.shiftingShapes) colors.onSurface else colors.surface
        first || appearance.palette == BoardPalette.THEME -> colors.primary
        else -> colors.tertiary
    }
    val badgeColor = remember(accent, badgeBase) { accent?.toneLike(badgeBase) ?: badgeBase }
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, motion.fastSpatialSpec(), label = "panel press")
    val animatedBackground by animateColorAsState(backgroundTarget, motion.fastEffectsSpec(), label = "serving surface")
    val animatedContent by animateColorAsState(foregroundTarget, motion.fastEffectsSpec(), label = "serving content")
    val highContrast = appearance.palette == BoardPalette.HIGH_CONTRAST
    val background = if (highContrast) backgroundTarget else animatedBackground
    val content = when {
        appearance.shiftingShapes -> panelForeground(
            preferred = foregroundTarget,
            fallback = colors.onSurface,
            panelColor = backgroundTarget,
            behind = colors.surfaceContainerLow,
        )
        highContrast -> foregroundTarget
        else -> animatedContent
    }
    val actionLabel = stringResource(if (points == null) R.string.choose_server_action else R.string.award_point)
    Surface(
        onClick = {
            if (appearance.shiftingShapes) shapeStep = (shapeStep + 1) % scoreBackdropShapes.size
            onClick()
        },
        interactionSource = interactions,
        modifier = modifier.fillMaxSize().semantics {
            role = Role.Button
            onClick(label = actionLabel, action = null)
        },
        shape = MaterialTheme.shapes.extraLargeIncreased,
        color = if (appearance.shiftingShapes) colors.surfaceContainerLow else background,
        contentColor = content,
    ) {
        Box(Modifier.fillMaxSize()) {
            if (appearance.shiftingShapes) {
                ScoreBackdrop(
                    step = if (first) step else (step + 1) % scoreBackdropShapes.size,
                    color = background,
                    modifier = Modifier.matchParentSize().scale(scale),
                )
            }
            if (points == null) {
                Column(
                    Modifier.fillMaxSize().scale(scale).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    PlayerMark(first, content, Modifier.size(52.dp))
                    Text(
                        name,
                        Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.headlineLargeEmphasized,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().scale(scale).padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        PlayerMark(first, content, Modifier.size(26.dp))
                        Text(
                            name,
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        AnimatedContent(
                            targetState = points,
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = {
                                if (appearance.animatePoints) {
                                    (fadeIn(motion.fastEffectsSpec()) + scaleIn(motion.fastSpatialSpec(), initialScale = 0.91f)) togetherWith
                                        (fadeOut(motion.fastEffectsSpec()) + scaleOut(motion.fastSpatialSpec(), targetScale = 1.06f))
                                } else {
                                    EnterTransition.None togetherWith ExitTransition.None
                                }
                            },
                            contentAlignment = Alignment.Center,
                            label = "point change",
                        ) { value ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                ScoreNumerals(
                                    value, fontFamily, sevenSegment, content, numberLimit,
                                    modifier = if (sevenSegment) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.games_count, games), style = MaterialTheme.typography.labelLarge)
                        AnimatedVisibility(
                            visible = serving,
                            enter = fadeIn(motion.fastEffectsSpec()) + scaleIn(motion.fastSpatialSpec()),
                            exit = fadeOut(motion.fastEffectsSpec()) + scaleOut(motion.fastSpatialSpec()),
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = badgeColor,
                                contentColor = badgeColor.contrastingText(if (highContrast) colors.onSurface else colors.onPrimary),
                            ) {
                                Text(
                                    stringResource(R.string.serving),
                                    Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelLargeEmphasized,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeChip(label: String, active: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (active) colors.tertiaryContainer else colors.surfaceContainer,
        contentColor = if (active) colors.onTertiaryContainer else colors.onSurface,
    ) {
        Text(label, Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelLargeEmphasized)
    }
}

private fun panelForeground(preferred: Color, fallback: Color, panelColor: Color, behind: Color): Color {
    fun contrast(first: Color, second: Color): Float {
        val a = first.luminance()
        val b = second.luminance()
        return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
    }
    return listOf(preferred, fallback, Color.Black, Color.White).firstOrNull {
        contrast(it, panelColor) >= 4.5f && contrast(it, behind) >= 4.5f
    } ?: fallback
}


private val ScoreScale.fontLimit
    get() = when (this) {
        ScoreScale.COMPACT -> 84.sp
        ScoreScale.LARGE -> 140.sp
        ScoreScale.EXTRA_LARGE -> 184.sp
    }

private val ScoreScale.previewLimit
    get() = when (this) {
        ScoreScale.COMPACT -> 42.sp
        ScoreScale.LARGE -> 64.sp
        ScoreScale.EXTRA_LARGE -> 92.sp
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayerMark(first: Boolean, foreground: Color, modifier: Modifier) {
    val shape = if (first) MaterialShapes.Cookie4Sided else MaterialShapes.SoftBurst
    Box(
        modifier.background(foreground.copy(alpha = 0.16f), shape.toShape()),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_paddle),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.62f),
            tint = foreground,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val scoreBackdropShapes by lazy {
    listOf(
        MaterialShapes.Sunny, MaterialShapes.Puffy, MaterialShapes.Flower, MaterialShapes.SoftBurst,
        MaterialShapes.Cookie4Sided, MaterialShapes.PuffyDiamond, MaterialShapes.Heart,
        MaterialShapes.Clover4Leaf, MaterialShapes.Ghostish, MaterialShapes.Cookie9Sided,
        MaterialShapes.Boom, MaterialShapes.Bun, MaterialShapes.PixelCircle, MaterialShapes.SoftBoom,
        MaterialShapes.Pill, MaterialShapes.Circle, MaterialShapes.Cookie7Sided, MaterialShapes.Diamond,
        MaterialShapes.Fan, MaterialShapes.Clover8Leaf, MaterialShapes.VerySunny, MaterialShapes.Arch,
        MaterialShapes.Slanted, MaterialShapes.Cookie6Sided, MaterialShapes.Arrow, MaterialShapes.SemiCircle,
        MaterialShapes.Triangle, MaterialShapes.Oval, MaterialShapes.Pentagon, MaterialShapes.Square,
        MaterialShapes.PixelTriangle, MaterialShapes.ClamShell, MaterialShapes.Burst, MaterialShapes.Gem,
        MaterialShapes.Cookie12Sided,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScoreBackdrop(step: Int, color: Color, modifier: Modifier = Modifier) {
    val motion = MaterialTheme.motionScheme
    val progress = remember { Animatable(1f) }
    var destination by remember { mutableIntStateOf(step) }
    var morph by remember { mutableStateOf<Morph?>(null) }
    val path = remember { Path() }
    LaunchedEffect(step) {
        if (step != destination) {
            progress.snapTo(0f)
            morph = Morph(scoreBackdropShapes[destination], scoreBackdropShapes[step])
            destination = step
            progress.animateTo(1f, motion.fastSpatialSpec())
            morph = null
        }
    }
    val currentMorph = morph
    val artwork = if (currentMorph == null) {
        Modifier.background(color, scoreBackdropShapes[step].toShape())
    } else {
        Modifier.drawBehind {
            path.rewind()
            currentMorph.toPath(progress.value.coerceIn(0f, 1f), path)
            path.transform(Matrix().apply { scale(size.width, size.height) })
            val center = path.getBounds().center
            path.translate(Offset(size.width / 2f - center.x, size.height / 2f - center.y))
            drawPath(path, color)
        }
    }
    Box(modifier.then(artwork))
}

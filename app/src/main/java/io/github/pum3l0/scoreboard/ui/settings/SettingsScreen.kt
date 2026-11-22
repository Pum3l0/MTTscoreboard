package io.github.pum3l0.scoreboard.ui.settings

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pum3l0.scoreboard.R
import io.github.pum3l0.scoreboard.data.settings.AppLanguage
import io.github.pum3l0.scoreboard.data.settings.AppFont
import io.github.pum3l0.scoreboard.data.settings.BoardAppearance
import io.github.pum3l0.scoreboard.data.settings.BoardLayoutMode
import io.github.pum3l0.scoreboard.data.settings.BoardPalette
import io.github.pum3l0.scoreboard.data.settings.HapticLevel
import io.github.pum3l0.scoreboard.data.settings.DEFAULT_SPEECH_RATE
import io.github.pum3l0.scoreboard.data.settings.MAX_SPEECH_RATE
import io.github.pum3l0.scoreboard.data.settings.MIN_SPEECH_RATE
import io.github.pum3l0.scoreboard.data.settings.ScoreFont
import io.github.pum3l0.scoreboard.data.settings.SpeechStatus
import io.github.pum3l0.scoreboard.data.settings.ScoreScale
import io.github.pum3l0.scoreboard.data.settings.ThemeMode
import io.github.pum3l0.scoreboard.data.settings.VoiceVariant
import io.github.pum3l0.scoreboard.data.settings.ThemeSettings
import io.github.pum3l0.scoreboard.ui.components.AccentButton
import io.github.pum3l0.scoreboard.ui.components.AdaptiveContent
import io.github.pum3l0.scoreboard.ui.match.ScoreNumerals
import io.github.pum3l0.scoreboard.ui.match.ScoreboardPreview
import io.github.pum3l0.scoreboard.ui.theme.appFontFamily
import io.github.pum3l0.scoreboard.ui.theme.scoreFontFamily
import java.io.File

enum class SettingsPage(val title: Int, val summary: Int, val icon: Int) {
    OVERVIEW(R.string.settings, R.string.settings_overview_hint, R.drawable.ic_settings),
    APP(R.string.app_appearance, R.string.app_appearance_summary, R.drawable.ic_appearance),
    BOARD(R.string.board_appearance, R.string.board_appearance_summary, R.drawable.ic_board),
    HAPTICS(R.string.sound_haptics, R.string.haptics_section_summary, R.drawable.ic_haptic),
    LANGUAGE(R.string.app_language, R.string.app_language_summary, R.drawable.ic_language),
    ABOUT(R.string.about, R.string.about_summary, R.drawable.ic_info),
}

@Composable
fun SettingsScreen(
    page: SettingsPage,
    settings: ThemeSettings,
    scoreFamily: FontFamily,
    onNavigate: (SettingsPage) -> Unit,
    onChange: (ThemeSettings) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onPickFont: () -> Unit,
    findFontFile: (String?) -> File?,
    onTestHaptics: () -> Unit,
    onOpenRules: () -> Unit,
    speechStatus: SpeechStatus,
    onTestSpeech: () -> Unit,
) {
    val motion = MaterialTheme.motionScheme
    AnimatedContent(
        targetState = page,
        transitionSpec = {
            (fadeIn(motion.fastEffectsSpec()) + scaleIn(motion.fastSpatialSpec(), initialScale = 0.97f)) togetherWith
                fadeOut(motion.fastEffectsSpec())
        },
        label = "settings section",
    ) { current ->
        val header: @Composable () -> Unit = {
            SectionHeader(current, onBack = { onNavigate(SettingsPage.OVERVIEW) })
        }
        when (current) {
            SettingsPage.OVERVIEW -> SettingsOverview(onNavigate, onOpenRules)
            SettingsPage.APP -> AppAppearanceScreen(settings, onChange, header)
            SettingsPage.BOARD -> BoardAppearanceScreen(
                settings, scoreFamily, onChange, onPickFont, findFontFile,
                onBack = { onNavigate(SettingsPage.OVERVIEW) },
            )
            SettingsPage.HAPTICS -> HapticsScreen(settings, onChange, onTestHaptics, speechStatus, onTestSpeech, header)
            SettingsPage.LANGUAGE -> AdaptiveContent(
                first = {
                    header()
                    Text(stringResource(R.string.app_language_summary))
                },
                second = {
                    OptionButtons(AppLanguage.entries, settings.language ?: AppLanguage.ENGLISH, onLanguageSelected) {
                        it.displayName
                    }
                },
            )
            SettingsPage.ABOUT -> AboutScreen(header)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsOverview(onNavigate: (SettingsPage) -> Unit, onOpenRules: () -> Unit) {
    AdaptiveContent(
        first = {
            val colors = MaterialTheme.colorScheme
            Surface(
                shape = MaterialTheme.shapes.extraExtraLarge,
                color = colors.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionMark(SettingsPage.OVERVIEW, colors.primary, colors.onPrimary, Modifier.size(58.dp))
                    Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineLargeEmphasized)
                }
            }
        },
        second = {
            SettingsGroup(
                R.string.settings_appearance_group, listOf(SettingsPage.BOARD, SettingsPage.APP), onNavigate,
            )
            SettingsGroup(
                R.string.settings_general_group, listOf(SettingsPage.LANGUAGE, SettingsPage.HAPTICS, SettingsPage.ABOUT), onNavigate,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SettingTitle(R.string.help_group)
                SegmentedListItem(
                    onClick = onOpenRules,
                    shapes = ListItemDefaults.segmentedShapes(0, 1),
                    leadingContent = {
                        val colors = MaterialTheme.colorScheme
                        Box(
                            Modifier.size(44.dp).background(colors.tertiaryContainer, MaterialShapes.Cookie4Sided.toShape()),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painterResource(R.drawable.ic_rules), null, tint = colors.onTertiaryContainer)
                        }
                    },
                    supportingContent = { Text(stringResource(R.string.rules_settings_summary)) },
                    trailingContent = { Icon(painterResource(R.drawable.ic_chevron), contentDescription = null) },
                ) { Text(stringResource(R.string.rules_link), style = MaterialTheme.typography.titleMediumEmphasized) }
            }
        },
    )
}

@Composable
private fun SettingsGroup(title: Int, pages: List<SettingsPage>, onNavigate: (SettingsPage) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SettingTitle(title)
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            pages.forEachIndexed { index, page ->
                val colors = MaterialTheme.colorScheme
                val tint = when (page) {
                    SettingsPage.BOARD -> colors.tertiaryContainer to colors.onTertiaryContainer
                    SettingsPage.HAPTICS -> colors.secondaryContainer to colors.onSecondaryContainer
                    else -> colors.primaryContainer to colors.onPrimaryContainer
                }
                SegmentedListItem(
                    onClick = { onNavigate(page) },
                    shapes = ListItemDefaults.segmentedShapes(index, pages.size),
                    leadingContent = { SectionMark(page, tint.first, tint.second, Modifier.size(44.dp)) },
                    supportingContent = { Text(stringResource(page.summary)) },
                    trailingContent = { Icon(painterResource(R.drawable.ic_chevron), contentDescription = null) },
                ) { Text(stringResource(page.title), style = MaterialTheme.typography.titleMediumEmphasized) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SectionMark(page: SettingsPage, background: Color, foreground: Color, modifier: Modifier) {
    val shape = when (page) {
        SettingsPage.OVERVIEW -> MaterialShapes.Cookie9Sided
        SettingsPage.APP -> MaterialShapes.Cookie4Sided
        SettingsPage.BOARD -> MaterialShapes.Puffy
        SettingsPage.HAPTICS -> MaterialShapes.SoftBurst
        SettingsPage.LANGUAGE -> MaterialShapes.Cookie4Sided
        SettingsPage.ABOUT -> MaterialShapes.Circle
    }
    Box(modifier.background(background, shape.toShape()), contentAlignment = Alignment.Center) {
        Icon(painterResource(page.icon), contentDescription = null, tint = foreground, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun SectionHeader(page: SettingsPage, onBack: () -> Unit) {
    TextButton(onClick = onBack, shapes = ButtonDefaults.shapes()) {
        Icon(painterResource(R.drawable.ic_back), contentDescription = null, modifier = Modifier.size(20.dp))
        Text(stringResource(R.string.settings), modifier = Modifier.padding(start = 4.dp))
    }
    val colors = MaterialTheme.colorScheme
    SectionMark(page, colors.tertiaryContainer, colors.onTertiaryContainer, Modifier.size(58.dp))
    Text(stringResource(page.title), style = MaterialTheme.typography.headlineLargeEmphasized)
}

@Composable
private fun AppAppearanceScreen(settings: ThemeSettings, onChange: (ThemeSettings) -> Unit, header: @Composable () -> Unit) {
    val canUseDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    AdaptiveContent(
        first = {
            header()
            SettingTitle(R.string.theme_mode)
            OptionButtons(ThemeMode.entries, settings.mode, { onChange(settings.copy(mode = it)) }) {
                stringResource(
                    when (it) {
                        ThemeMode.SYSTEM -> R.string.theme_system
                        ThemeMode.LIGHT -> R.string.theme_light
                        ThemeMode.DARK -> R.string.theme_dark
                    },
                )
            }
            SettingToggle(
                title = R.string.dynamic_colors,
                summary = when {
                    settings.appAccent != null -> R.string.accent_overrides_dynamic
                    canUseDynamic -> R.string.dynamic_info
                    else -> R.string.dynamic_unavailable
                },
                checked = canUseDynamic && settings.dynamicColor && settings.appAccent == null,
                enabled = canUseDynamic && settings.appAccent == null,
                onChange = { onChange(settings.copy(dynamicColor = it)) },
            )
            AccentSetting(R.string.app_accent, settings.appAccent, MaterialTheme.colorScheme.primary, settings.recentColors) {
                onChange(settings.copy(appAccent = it))
            }
        },
        second = {
            SettingTitle(R.string.app_font)
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialTheme.shapes.extraLargeIncreased,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.home_title),
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                )
            }
            Column(
                Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                AppFont.entries.forEachIndexed { index, font ->
                    val (name, description) = when (font) {
                        AppFont.NUNITO -> "Nunito" to R.string.font_rounded
                        AppFont.SOFT_SANS -> "Adwaita Sans" to R.string.font_soft
                        AppFont.SYSTEM -> "Roboto" to R.string.font_system
                        AppFont.OPEN_SANS -> "Open Sans" to R.string.font_classic
                        AppFont.SANS_CODE -> "Google Sans Code" to R.string.font_mono
                        AppFont.HEAVY -> "Roboto Black" to R.string.font_heavy
                        AppFont.SERIF -> "Noto Serif" to R.string.font_serif
                    }
                    val family = appFontFamily(font)
                    SegmentedListItem(
                        selected = font == settings.appFont,
                        onClick = { onChange(settings.copy(appFont = font)) },
                        shapes = ListItemDefaults.segmentedShapes(index, AppFont.entries.size),
                        leadingContent = {
                            Text(stringResource(R.string.font_sample), style = MaterialTheme.typography.titleLarge.copy(fontFamily = family))
                        },
                        supportingContent = { Text(stringResource(description)) },
                        trailingContent = { RadioButton(selected = font == settings.appFont, onClick = null) },
                    ) { Text(name) }
                }
            }
        },
    )
}

private enum class BoardSection(val label: Int) {
    PANELS(R.string.board_section_panels),
    DIGITS(R.string.board_section_digits),
    BEHAVIOR(R.string.board_section_behavior),
}

@Composable
private fun BoardAppearanceScreen(
    settings: ThemeSettings,
    scoreFamily: FontFamily,
    onChange: (ThemeSettings) -> Unit,
    onPickFont: () -> Unit,
    findFontFile: (String?) -> File?,
    onBack: () -> Unit,
) {
    val board = settings.board
    val changeBoard: (BoardAppearance) -> Unit = { onChange(settings.copy(board = it)) }
    val preview: @Composable () -> Unit = {
        Text(stringResource(R.string.preview_board_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
        ScoreboardPreview(board, scoreFamily, settings.scoreFont == ScoreFont.SEVEN_SEGMENT, Modifier.fillMaxWidth())
    }
    var section by rememberSaveable { mutableStateOf(BoardSection.PANELS) }
    val controls: @Composable () -> Unit = {
        OptionButtons(BoardSection.entries, section, { section = it }) { stringResource(it.label) }
        val motion = MaterialTheme.motionScheme
        AnimatedContent(
            targetState = section,
            transitionSpec = { fadeIn(motion.fastEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec()) },
            label = "board settings category",
        ) { selected ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (selected) {
                    BoardSection.PANELS -> BoardControls(board, settings.recentColors, changeBoard)
                    BoardSection.DIGITS -> {
                        ScoreSizeControl(board, changeBoard)
                        ScoreFonts(settings, onChange, onPickFont, findFontFile)
                    }
                    BoardSection.BEHAVIOR -> {
                        BoardMotionControls(board, changeBoard)
                        TimeoutSettings(settings, onChange)
                    }
                }
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pinPreview = maxWidth < 600.dp && maxHeight >= 570.dp && LocalDensity.current.fontScale <= 1.35f
        if (pinPreview) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = stringResource(R.string.back))
                    }
                    Text(
                        stringResource(R.string.board_appearance),
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                    )
                }
                preview()
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    controls()
                }
            }
        } else {
            AdaptiveContent(
                first = {
                    SectionHeader(SettingsPage.BOARD, onBack)
                    preview()
                },
                second = { controls() },
            )
        }
    }
}

@Composable
private fun BoardControls(
    board: BoardAppearance,
    recentColors: List<Int>,
    changeBoard: (BoardAppearance) -> Unit,
) {
    SettingTitle(R.string.board_palette)
    OptionButtons(BoardPalette.entries, board.palette, { changeBoard(board.copy(palette = it)) }) {
        stringResource(
            when (it) {
                BoardPalette.TWO_TONE -> R.string.palette_two_tone
                BoardPalette.THEME -> R.string.palette_theme
                BoardPalette.HIGH_CONTRAST -> R.string.palette_high_contrast
            },
        )
    }
    val colors = MaterialTheme.colorScheme
    val custom = board.colors
    when (board.palette) {
        BoardPalette.TWO_TONE -> {
            AccentSetting(R.string.panel_first_accent, custom.firstAccent, colors.primary, recentColors) {
                changeBoard(board.copy(colors = custom.copy(firstAccent = it)))
            }
            AccentSetting(R.string.panel_second_accent, custom.secondAccent, colors.tertiary, recentColors) {
                changeBoard(board.copy(colors = custom.copy(secondAccent = it)))
            }
        }
        BoardPalette.THEME -> AccentSetting(R.string.panel_theme_accent, custom.themeAccent, colors.primary, recentColors) {
            changeBoard(board.copy(colors = custom.copy(themeAccent = it)))
        }
        BoardPalette.HIGH_CONTRAST -> AccentSetting(R.string.panel_contrast_accent, custom.contrastAccent, colors.onSurface, recentColors) {
            changeBoard(board.copy(colors = custom.copy(contrastAccent = it)))
        }
    }
    SettingTitle(R.string.board_layout)
    OptionButtons(BoardLayoutMode.entries, board.layout, { changeBoard(board.copy(layout = it)) }) {
        stringResource(
            when (it) {
                BoardLayoutMode.AUTO -> R.string.layout_auto
                BoardLayoutMode.SIDE_BY_SIDE -> R.string.layout_side_by_side
                BoardLayoutMode.STACKED -> R.string.layout_stacked
            },
        )
    }
}

@Composable
private fun ScoreSizeControl(board: BoardAppearance, changeBoard: (BoardAppearance) -> Unit) {
    SettingTitle(R.string.score_scale)
    OptionButtons(ScoreScale.entries, board.scale, { changeBoard(board.copy(scale = it)) }) {
        stringResource(
            when (it) {
                ScoreScale.COMPACT -> R.string.scale_compact
                ScoreScale.LARGE -> R.string.scale_large
                ScoreScale.EXTRA_LARGE -> R.string.scale_extra_large
            },
        )
    }
}

@Composable
private fun BoardMotionControls(board: BoardAppearance, changeBoard: (BoardAppearance) -> Unit) {
    SettingTitle(R.string.board_motion)
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        SettingToggle(
            R.string.shift_shapes, R.string.shift_shapes_summary, board.shiftingShapes,
            index = 0, count = 4, onChange = { changeBoard(board.copy(shiftingShapes = it)) },
        )
        SettingToggle(
            R.string.animate_points, R.string.animate_points_summary, board.animatePoints,
            index = 1, count = 4, onChange = { changeBoard(board.copy(animatePoints = it)) },
        )
        SettingToggle(
            R.string.keep_screen, R.string.keep_screen_summary, board.keepScreenOn,
            index = 2, count = 4, onChange = { changeBoard(board.copy(keepScreenOn = it)) },
        )
        SettingToggle(
            R.string.elapsed_toggle, R.string.elapsed_toggle_summary, board.showElapsed,
            index = 3, count = 4, onChange = { changeBoard(board.copy(showElapsed = it)) },
        )
    }
}

@Composable
private fun TimeoutSettings(settings: ThemeSettings, onChange: (ThemeSettings) -> Unit) {
    var editingDuration by rememberSaveable { mutableStateOf(false) }
    SettingTitle(R.string.timeout_title)
    SettingToggle(
        R.string.timeout_enable, R.string.timeout_enable_summary, settings.timeoutEnabled,
        onChange = { onChange(settings.copy(timeoutEnabled = it)) },
    )
    if (settings.timeoutEnabled) {
        val seconds = (listOf(15, 30, 45, 60, 90, 120, 180) + settings.timeoutSeconds).distinct().sorted()
        OptionButtons(seconds, settings.timeoutSeconds, { onChange(settings.copy(timeoutSeconds = it)) }) {
            stringResource(R.string.timeout_seconds, it)
        }
        TextButton(onClick = { editingDuration = true }, shapes = ButtonDefaults.shapes()) {
            Text(stringResource(R.string.timeout_custom))
        }
        SettingToggle(
            R.string.timeout_signal, R.string.timeout_signal_summary, settings.timeoutSignal,
            onChange = { onChange(settings.copy(timeoutSignal = it)) },
        )
    }
    if (editingDuration) {
        var secondsText by rememberSaveable { mutableStateOf(settings.timeoutSeconds.toString()) }
        val seconds = secondsText.toIntOrNull()?.takeIf { it in 15..180 }
        AlertDialog(
            onDismissRequest = { editingDuration = false },
            title = { Text(stringResource(R.string.timeout_custom)) },
            text = {
                OutlinedTextField(
                    value = secondsText,
                    onValueChange = { secondsText = it.filter(Char::isDigit).take(3) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.timeout_duration_label)) },
                    supportingText = { Text(stringResource(R.string.timeout_duration_range)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = seconds == null,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = seconds != null,
                    shapes = ButtonDefaults.shapes(),
                    onClick = {
                        if (seconds != null) onChange(settings.copy(timeoutSeconds = seconds))
                        editingDuration = false
                    },
                ) { Text(stringResource(R.string.save_accent)) }
            },
            dismissButton = {
                TextButton(onClick = { editingDuration = false }, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ScoreFonts(
    settings: ThemeSettings,
    onChange: (ThemeSettings) -> Unit,
    onPickFont: () -> Unit,
    findFontFile: (String?) -> File?,
) {
    SettingTitle(R.string.score_font)
    Column(
        Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        val fonts = ScoreFont.entries.filter { it != ScoreFont.CUSTOM }.map { it to null } +
            settings.customFonts.map { ScoreFont.CUSTOM to it }
        fonts.forEachIndexed { index, (font, custom) ->
            val name = when (font) {
                ScoreFont.ROBOTO_BLACK -> "Roboto Black"
                ScoreFont.ROBOTO_MONO -> "Roboto Mono"
                ScoreFont.NOTO_SERIF -> "Noto Serif"
                ScoreFont.ROBOTO_CONDENSED -> "Roboto Condensed"
                ScoreFont.OPEN_SANS -> "Open Sans ExtraBold"
                ScoreFont.SANS_CODE -> "Google Sans Code"
                ScoreFont.PIXELIFY -> "Pixelify Sans"
                ScoreFont.SEVEN_SEGMENT -> stringResource(R.string.font_segmented)
                ScoreFont.CUSTOM -> custom?.name?.ifBlank { null } ?: stringResource(R.string.custom_font)
            }
            val selected = font == settings.scoreFont && (custom == null || custom.file == settings.customFontFile)
            val file = remember(custom?.file) { findFontFile(custom?.file) }
            val sampleFamily = scoreFontFamily(font, file)
            SegmentedListItem(
                selected = selected,
                onClick = { onChange(settings.copy(scoreFont = font, customFontFile = custom?.file)) },
                shapes = ListItemDefaults.segmentedShapes(index, fonts.size),
                leadingContent = {
                    Box(Modifier.width(44.dp).height(28.dp), contentAlignment = Alignment.Center) {
                        ScoreNumerals(
                            28, sampleFamily, font == ScoreFont.SEVEN_SEGMENT,
                            MaterialTheme.colorScheme.onSurface, 26.sp,
                            modifier = if (font == ScoreFont.SEVEN_SEGMENT) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
                        )
                    }
                },
                trailingContent = { RadioButton(selected = selected, onClick = null) },
            ) { Text(name, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
    }
    AccentButton(onClick = onPickFont, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.load_font))
    }
}

@Composable
private fun HapticsScreen(
    settings: ThemeSettings,
    onChange: (ThemeSettings) -> Unit,
    onTest: () -> Unit,
    speechStatus: SpeechStatus,
    onTestSpeech: () -> Unit,
    header: @Composable () -> Unit,
) {
    AdaptiveContent(
        first = {
            header()
            Text(stringResource(R.string.haptics_summary), color = MaterialTheme.colorScheme.onSurfaceVariant)
            SettingTitle(R.string.haptic_strength)
            OptionButtons(
                HapticLevel.entries,
                settings.hapticLevel,
                { onChange(settings.copy(hapticLevel = it)) },
            ) { level ->
                if (level == HapticLevel.OFF) stringResource(R.string.haptic_off) else level.ordinal.toString()
            }
        },
        second = {
            AccentButton(onClick = onTest, enabled = settings.hapticLevel != HapticLevel.OFF) {
                Text(stringResource(R.string.test_haptics))
            }
            Text(stringResource(R.string.haptics_system_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
            SettingTitle(R.string.voice_section)
            val summary = when (speechStatus) {
                SpeechStatus.OFF -> R.string.voice_summary
                SpeechStatus.STARTING -> R.string.voice_loading
                SpeechStatus.DOWNLOADING -> R.string.voice_downloading
                SpeechStatus.READY -> R.string.voice_ready
                SpeechStatus.TUNED -> R.string.voice_tuned
                SpeechStatus.UNAVAILABLE -> R.string.voice_unavailable
            }
            SettingToggle(
                title = R.string.voice_score,
                summary = R.string.voice_score_hint,
                checked = settings.announceScore,
                onChange = { onChange(settings.copy(announceScore = it)) },
            )
            SettingToggle(
                title = R.string.voice_server,
                summary = R.string.voice_server_hint,
                checked = settings.announceServer,
                onChange = { onChange(settings.copy(announceServer = it)) },
            )
            if (settings.announceScore || settings.announceServer) {
                Text(stringResource(summary), color = MaterialTheme.colorScheme.onSurfaceVariant)
                OptionButtons(VoiceVariant.entries, settings.scoreVoice, { onChange(settings.copy(scoreVoice = it)) }) {
                    stringResource(if (it == VoiceVariant.MALE) R.string.voice_male else R.string.voice_female)
                }
                SpeechRateControl(settings.speechRate) { onChange(settings.copy(speechRate = it)) }
            }
            if (speechStatus == SpeechStatus.DOWNLOADING) LinearWavyProgressIndicator(Modifier.fillMaxWidth())
            AccentButton(
                onClick = onTestSpeech,
                enabled = speechStatus == SpeechStatus.READY || speechStatus == SpeechStatus.TUNED,
            ) {
                Text(stringResource(R.string.voice_test))
            }
        },
    )
}

@Composable
private fun SpeechRateControl(rate: Int, onChange: (Int) -> Unit) {
    val label = stringResource(R.string.voice_rate, rate)
    val slider = remember {
        SliderState(rate.toFloat(), 5, MIN_SPEECH_RATE.toFloat()..MAX_SPEECH_RATE.toFloat())
    }
    SideEffect { slider.value = rate.toFloat() }
    Text(label, style = MaterialTheme.typography.labelLarge)
    Slider(
        state = slider,
        onValueChange = {
            slider.value = it
            onChange(kotlin.math.round(it).toInt())
        },
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
    )
    if (rate != DEFAULT_SPEECH_RATE) {
        TextButton(onClick = { onChange(DEFAULT_SPEECH_RATE) }, shapes = ButtonDefaults.shapes()) {
            Text(stringResource(R.string.voice_rate_reset))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AboutScreen(header: @Composable () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val version = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    val linkError = stringResource(R.string.link_error)
    val open: (String) -> Unit = { url ->
        runCatching { uriHandler.openUri(url) }
            .onFailure { Toast.makeText(context, linkError, Toast.LENGTH_SHORT).show() }
    }
    AdaptiveContent(
        first = {
            header()
            Surface(
                shape = MaterialTheme.shapes.extraLargeIncreased,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painterResource(R.drawable.author_avatar),
                        contentDescription = null,
                        modifier = Modifier.size(72.dp).clip(MaterialShapes.Cookie9Sided.toShape()),
                        contentScale = ContentScale.Crop,
                    )
                    Column {
                        Text(stringResource(R.string.author), style = MaterialTheme.typography.labelLarge)
                        Text("Pum3l0", style = MaterialTheme.typography.titleLargeEmphasized)
                    }
                }
            }
        },
        second = {
            SettingTitle(R.string.about_links)
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                for ((index, link) in listOf(
                    "pumelo.site" to "https://pumelo.site",
                    "GitHub · Pum3l0" to "https://github.com/Pum3l0",
                ).withIndex()) {
                    SegmentedListItem(
                        onClick = { open(link.second) },
                        shapes = ListItemDefaults.segmentedShapes(index, 2),
                        trailingContent = { Icon(painterResource(R.drawable.ic_external), contentDescription = null) },
                    ) { Text(link.first) }
                }
            }
            Text(stringResource(R.string.about_version, version), color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
    )
}

@Composable
private fun SettingTitle(id: Int) {
    Text(stringResource(id), style = MaterialTheme.typography.titleMediumEmphasized)
}

@Composable
private fun SettingToggle(
    title: Int,
    summary: Int,
    checked: Boolean,
    enabled: Boolean = true,
    index: Int = 0,
    count: Int = 1,
    onChange: (Boolean) -> Unit,
) {
    SegmentedListItem(
        checked = checked,
        enabled = enabled,
        onCheckedChange = onChange,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        supportingContent = { Text(stringResource(summary)) },
        trailingContent = { Switch(checked = checked, enabled = enabled, onCheckedChange = null) },
    ) { Text(stringResource(title)) }
}

@Composable
private fun <T> OptionButtons(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    enabled: Boolean = true,
    label: @Composable (T) -> String,
) {
    FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            FilledTonalToggleButton(
                checked = option == selected,
                onCheckedChange = { onSelect(option) },
                enabled = enabled,
            ) { Text(label(option)) }
        }
    }
}

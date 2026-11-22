package io.github.pum3l0.scoreboard.data.settings

import android.content.ContentResolver
import android.content.SharedPreferences
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal const val MIN_SPEECH_RATE = 70
internal const val DEFAULT_SPEECH_RATE = 100
internal const val MAX_SPEECH_RATE = 130
private const val RECENT_COLOR_LIMIT = 8

internal const val SETTINGS_FILE = "scoreboard_settings"
private const val LANGUAGE_KEY = "app_language"

enum class AppLanguage(val tag: String, val displayName: String) {
    ENGLISH("en-US", "English"),
    RUSSIAN("ru-RU", "Русский");

    val locale: Locale get() = Locale.forLanguageTag(tag)

    companion object {
        internal fun stored(preferences: SharedPreferences): AppLanguage? =
            entries.firstOrNull { it.tag == preferences.getString(LANGUAGE_KEY, null) }

        fun forLocale(locale: Locale): AppLanguage =
            if (locale.language == "ru") RUSSIAN else ENGLISH
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class ScoreFont {
    ROBOTO_BLACK, ROBOTO_MONO, NOTO_SERIF, ROBOTO_CONDENSED, OPEN_SANS, SANS_CODE, PIXELIFY, SEVEN_SEGMENT, CUSTOM,
}

data class ImportedFont(val file: String, val name: String)

data class ThemeSettings(
    val language: AppLanguage? = null,
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val appAccent: Int? = null,
    val recentColors: List<Int> = emptyList(),
    val appFont: AppFont = AppFont.NUNITO,
    val board: BoardAppearance = BoardAppearance(),
    val scoreFont: ScoreFont = ScoreFont.ROBOTO_BLACK,
    val customFonts: List<ImportedFont> = emptyList(),
    val customFontFile: String? = null,
    val hapticLevel: HapticLevel = HapticLevel.TWO,
    val announceScore: Boolean = false,
    val announceServer: Boolean = false,
    val scoreVoice: VoiceVariant = VoiceVariant.MALE,
    val speechRate: Int = DEFAULT_SPEECH_RATE,
    val timeoutEnabled: Boolean = false,
    val timeoutSeconds: Int = 60,
    val timeoutSignal: Boolean = true,
)

class ThemePreferences(private val preferences: SharedPreferences, private val fontsDir: File) {
    private val mutableSettings = MutableStateFlow(
        ThemeSettings(
            language = AppLanguage.stored(preferences),
            mode = readEnum("theme", ThemeMode.SYSTEM),
            dynamicColor = preferences.getBoolean("dynamic_color", true),
            appAccent = readColor("app_accent"),
            recentColors = preferences.getString("recent_colors", null).orEmpty().split(',')
                .mapNotNull(String::toIntOrNull).distinct().take(RECENT_COLOR_LIMIT),
            appFont = readEnum("app_font", AppFont.NUNITO),
            board = BoardAppearance(
                palette = readEnum("board_palette", BoardPalette.TWO_TONE),
                layout = readEnum("board_layout", BoardLayoutMode.AUTO),
                scale = readEnum("board_scale", ScoreScale.LARGE),
                shiftingShapes = preferences.getBoolean("shifting_shapes", true),
                animatePoints = preferences.getBoolean("animate_points", true),
                keepScreenOn = preferences.getBoolean("keep_screen_on", true),
                showElapsed = preferences.getBoolean("show_elapsed", false),
                colors = BoardColors(
                    firstAccent = readColor("panel_accent_first"),
                    secondAccent = readColor("panel_accent_second"),
                    themeAccent = readColor("panel_accent_theme"),
                    contrastAccent = readColor("panel_accent_contrast"),
                ),
            ),
            scoreFont = readEnum("score_font", ScoreFont.ROBOTO_BLACK),
            customFonts = loadFonts(),
            customFontFile = preferences.getString("custom_font_file", null),
            hapticLevel = readEnum("haptic_level", legacyHaptics()),
            announceScore = preferences.getBoolean("announce_score", false),
            announceServer = preferences.getBoolean("announce_server", false),
            scoreVoice = readEnum("score_voice", VoiceVariant.MALE),
            speechRate = preferences.getInt("speech_rate", DEFAULT_SPEECH_RATE)
                .coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE),
            timeoutEnabled = preferences.getBoolean("timeout_enabled", false),
            timeoutSeconds = preferences.getInt("timeout_seconds", 60).coerceIn(15, 180),
            timeoutSignal = preferences.getBoolean("timeout_signal", true),
        ),
    )
    val settings = mutableSettings.asStateFlow()

    fun save(updated: ThemeSettings) {
        val previous = mutableSettings.value
        val newColors = accentColors(updated).zip(accentColors(previous))
            .mapNotNull { (color, old) -> color.takeIf { color != old } }
        val settings = updated.copy(
            recentColors = (newColors + previous.recentColors).distinct().take(RECENT_COLOR_LIMIT),
        )
        preferences.edit()
            .putString(LANGUAGE_KEY, settings.language?.tag)
            .putString("theme", settings.mode.name)
            .putBoolean("dynamic_color", settings.dynamicColor)
            .putString("app_accent", settings.appAccent?.toString())
            .putString("recent_colors", settings.recentColors.joinToString(","))
            .putString("app_font", settings.appFont.name)
            .putString("board_palette", settings.board.palette.name)
            .putString("board_layout", settings.board.layout.name)
            .putString("board_scale", settings.board.scale.name)
            .putBoolean("shifting_shapes", settings.board.shiftingShapes)
            .putBoolean("animate_points", settings.board.animatePoints)
            .putBoolean("keep_screen_on", settings.board.keepScreenOn)
            .putBoolean("show_elapsed", settings.board.showElapsed)
            .putString("panel_accent_first", settings.board.colors.firstAccent?.toString())
            .putString("panel_accent_second", settings.board.colors.secondAccent?.toString())
            .putString("panel_accent_theme", settings.board.colors.themeAccent?.toString())
            .putString("panel_accent_contrast", settings.board.colors.contrastAccent?.toString())
            .putString("score_font", settings.scoreFont.name)
            .putString("custom_fonts", encodeFonts(settings.customFonts))
            .putString("custom_font_file", settings.customFontFile)
            .putString("haptic_level", settings.hapticLevel.name)
            .putBoolean("announce_score", settings.announceScore)
            .putBoolean("announce_server", settings.announceServer)
            .putString("score_voice", settings.scoreVoice.name)
            .putInt("speech_rate", settings.speechRate)
            .putBoolean("timeout_enabled", settings.timeoutEnabled)
            .putInt("timeout_seconds", settings.timeoutSeconds)
            .putBoolean("timeout_signal", settings.timeoutSignal)
            .apply()
        mutableSettings.value = settings
    }

    fun customFont(fileName: String?): File? =
        fileName?.let { File(fontsDir, it).takeIf(File::isFile) }

    suspend fun importFont(resolver: ContentResolver, uri: Uri) {
        val file = File(fontsDir, "font-${UUID.randomUUID()}.bin")
        try {
            val name = withContext(Dispatchers.IO) {
                fontsDir.mkdirs()
                checkNotNull(resolver.openInputStream(uri)).use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var total = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= 16L * 1024 * 1024)
                            output.write(buffer, 0, count)
                        }
                    }
                }
                requireNotNull(Typeface.Builder(file).build())
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                    ?.take(100)
                    .orEmpty()
            }
            save(
                settings.value.copy(
                    scoreFont = ScoreFont.CUSTOM,
                    customFonts = settings.value.customFonts + ImportedFont(file.name, name),
                    customFontFile = file.name,
                ),
            )
        } catch (error: Exception) {
            withContext(NonCancellable + Dispatchers.IO) { file.delete() }
            throw error
        }
    }

    private fun accentColors(settings: ThemeSettings): List<Int?> = with(settings.board.colors) {
        listOf(settings.appAccent, firstAccent, secondAccent, themeAccent, contrastAccent)
    }

    private fun readColor(key: String): Int? = preferences.getString(key, null)?.toIntOrNull()

    private fun legacyHaptics(): HapticLevel = when {
        !preferences.getBoolean("haptics", true) -> HapticLevel.OFF
        preferences.getString("haptic_style", null) == "DISTINCT" -> HapticLevel.THREE
        else -> HapticLevel.TWO
    }

    private inline fun <reified E : Enum<E>> readEnum(key: String, fallback: E): E {
        val name = preferences.getString(key, null)
        return enumValues<E>().firstOrNull { it.name == name } ?: fallback
    }

    private fun loadFonts(): List<ImportedFont> {
        val encoded = preferences.getString("custom_fonts", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(encoded)
            List(array.length()) { index ->
                val row = array.getJSONObject(index)
                ImportedFont(row.getString("file"), row.getString("name"))
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeFonts(fonts: List<ImportedFont>): String = JSONArray().apply {
        fonts.forEach { put(JSONObject().put("file", it.file).put("name", it.name)) }
    }.toString()
}

package io.github.pum3l0.scoreboard.data.settings

enum class AppFont { NUNITO, SOFT_SANS, SYSTEM, OPEN_SANS, SANS_CODE, HEAVY, SERIF }

enum class BoardPalette { TWO_TONE, THEME, HIGH_CONTRAST }

enum class BoardLayoutMode { AUTO, SIDE_BY_SIDE, STACKED }

enum class ScoreScale { COMPACT, LARGE, EXTRA_LARGE }

enum class HapticLevel(val amplitude: Int, val durationMillis: Long) {
    OFF(0, 0),
    ONE(55, 9),
    TWO(100, 14),
    THREE(165, 20),
    FOUR(235, 28),
}

data class BoardAppearance(
    val palette: BoardPalette = BoardPalette.TWO_TONE,
    val layout: BoardLayoutMode = BoardLayoutMode.AUTO,
    val scale: ScoreScale = ScoreScale.LARGE,
    val shiftingShapes: Boolean = true,
    val animatePoints: Boolean = true,
    val keepScreenOn: Boolean = true,
    val showElapsed: Boolean = false,
    val colors: BoardColors = BoardColors(),
)

data class BoardColors(
    val firstAccent: Int? = null,
    val secondAccent: Int? = null,
    val themeAccent: Int? = null,
    val contrastAccent: Int? = null,
) {
    fun forPlayer(palette: BoardPalette, first: Boolean): Int? = when (palette) {
        BoardPalette.TWO_TONE -> if (first) firstAccent else secondAccent
        BoardPalette.THEME -> themeAccent
        BoardPalette.HIGH_CONTRAST -> contrastAccent
    }
}

enum class VoiceVariant(val pitch: Float) { MALE(0.84f), FEMALE(1.22f) }

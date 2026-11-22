package io.github.pum3l0.scoreboard.ui.match

import java.util.Locale

internal fun elapsedTime(millis: Long): String {
    val seconds = millis.coerceAtLeast(0) / 1_000
    val hours = seconds / 3_600
    val minutes = seconds / 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes % 60, seconds % 60)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds % 60)
    }
}

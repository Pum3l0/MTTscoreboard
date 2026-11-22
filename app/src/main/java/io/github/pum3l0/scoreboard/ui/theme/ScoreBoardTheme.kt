package io.github.pum3l0.scoreboard.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.github.pum3l0.scoreboard.data.settings.AppFont

@Composable
fun ScoreBoardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    appFont: AppFont = AppFont.NUNITO,
    appAccent: Int? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val baseColors = when {
        appAccent == null && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> darkColorScheme()
        else -> expressiveLightColorScheme()
    }

    val colors = remember(baseColors, appAccent) { appAccent?.let { applyCustomAccent(baseColors, Color(it)) } ?: baseColors }
    val family = appFontFamily(appFont)
    val typography = remember(family) { Typography(fontFamily = family) }
    MaterialExpressiveTheme(colorScheme = colors, typography = typography, content = content)
}

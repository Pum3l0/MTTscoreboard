package io.github.pum3l0.scoreboard.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal fun applyCustomAccent(base: ColorScheme, seed: Color): ColorScheme {
    val primary = seed.toneLike(base.primary)
    val primaryContainer = seed.toneLike(base.primaryContainer)
    val secondary = seed.toneLike(base.secondary, saturationScale = 0.60f)
    val secondaryContainer = seed.toneLike(base.secondaryContainer, saturationScale = 0.52f)
    val tertiary = seed.toneLike(base.tertiary, hueShift = 42f, saturationScale = 0.85f)
    val tertiaryContainer = seed.toneLike(base.tertiaryContainer, hueShift = 42f, saturationScale = 0.78f)
    val surface = seed.toneLike(base.surface, saturationScale = 0.10f)
    val surfaceVariant = seed.toneLike(base.surfaceVariant, saturationScale = 0.15f)
    val inverse = seed.toneLike(base.inverseSurface, saturationScale = 0.10f)
    return base.copy(
        primary = primary, onPrimary = primary.contrastingText(base.onPrimary),
        primaryContainer = primaryContainer, onPrimaryContainer = primaryContainer.contrastingText(base.onPrimaryContainer),
        secondary = secondary, onSecondary = secondary.contrastingText(base.onSecondary),
        secondaryContainer = secondaryContainer, onSecondaryContainer = secondaryContainer.contrastingText(base.onSecondaryContainer),
        tertiary = tertiary, onTertiary = tertiary.contrastingText(base.onTertiary),
        tertiaryContainer = tertiaryContainer, onTertiaryContainer = tertiaryContainer.contrastingText(base.onTertiaryContainer),
        background = surface, onBackground = surface.contrastingText(base.onBackground),
        surface = surface, onSurface = surface.contrastingText(base.onSurface),
        surfaceVariant = surfaceVariant, onSurfaceVariant = surfaceVariant.contrastingText(base.onSurfaceVariant),
        surfaceTint = primary,
        surfaceContainerLowest = seed.toneLike(base.surfaceContainerLowest, saturationScale = 0.10f),
        surfaceContainerLow = seed.toneLike(base.surfaceContainerLow, saturationScale = 0.12f),
        surfaceContainer = seed.toneLike(base.surfaceContainer, saturationScale = 0.12f),
        surfaceContainerHigh = seed.toneLike(base.surfaceContainerHigh, saturationScale = 0.14f),
        surfaceContainerHighest = seed.toneLike(base.surfaceContainerHighest, saturationScale = 0.14f),
        surfaceBright = seed.toneLike(base.surfaceBright, saturationScale = 0.12f),
        surfaceDim = seed.toneLike(base.surfaceDim, saturationScale = 0.12f),
        inverseSurface = inverse, inverseOnSurface = inverse.contrastingText(base.inverseOnSurface),
        inversePrimary = seed.toneLike(base.inversePrimary),
        outline = seed.toneLike(base.outline, saturationScale = 0.16f),
        outlineVariant = seed.toneLike(base.outlineVariant, saturationScale = 0.15f),
    )
}

internal fun Color.toneLike(reference: Color, hueShift: Float = 0f, saturationScale: Float = 1f): Color {
    val max = max(red, max(green, blue))
    val min = min(red, min(green, blue))
    val delta = max - min
    val lightness = (max + min) / 2f
    val hue = when {
        delta == 0f -> 0f
        max == red -> 60f * ((green - blue) / delta)
        max == green -> 60f * (2f + (blue - red) / delta)
        else -> 60f * (4f + (red - green) / delta)
    }
    val saturation = if (delta == 0f) 0f else delta / (1f - abs(2f * lightness - 1f))
    val target = reference.luminance()
    var low = 0f
    var high = 1f
    repeat(12) {
        val mid = (low + high) / 2f
        val tone = Color.hsl((hue + hueShift + 360f) % 360f, (saturation * saturationScale).coerceIn(0f, 1f), mid)
        if (tone.luminance() < target) low = mid else high = mid
    }
    return Color.hsl(
        (hue + hueShift + 360f) % 360f,
        (saturation * saturationScale).coerceIn(0f, 1f),
        (low + high) / 2f,
    )
}

internal fun Color.contrastingText(preferred: Color): Color {
    val background = luminance()
    val foreground = preferred.luminance()
    val contrast = (max(background, foreground) + 0.05f) / (min(background, foreground) + 0.05f)
    if (contrast >= 4.5f) return preferred
    return if (background >= 0.179f) Color.Black else Color.White
}

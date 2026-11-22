package io.github.pum3l0.scoreboard.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import io.github.pum3l0.scoreboard.R
import io.github.pum3l0.scoreboard.data.settings.ScoreFont
import io.github.pum3l0.scoreboard.data.settings.AppFont
import java.io.File

@Composable
fun scoreFontFamily(scoreFont: ScoreFont, customFont: File? = null): FontFamily =
    remember(scoreFont, customFont) {
        when (scoreFont) {
            ScoreFont.ROBOTO_BLACK -> FontFamily(Font(R.font.roboto_black, FontWeight.Black))
            ScoreFont.ROBOTO_MONO -> FontFamily(Font(R.font.roboto_mono_bold, FontWeight.Bold))
            ScoreFont.NOTO_SERIF -> FontFamily(Font(R.font.noto_serif_bold, FontWeight.Bold))
            ScoreFont.ROBOTO_CONDENSED -> FontFamily(Font(R.font.roboto_condensed_bold, FontWeight.Bold))
            ScoreFont.OPEN_SANS -> FontFamily(Font(R.font.open_sans_extra_bold, FontWeight.ExtraBold))
            ScoreFont.SEVEN_SEGMENT, ScoreFont.SANS_CODE -> FontFamily(Font(R.font.google_sans_code_bold, FontWeight.Bold))
            ScoreFont.PIXELIFY -> FontFamily(Font(R.font.pixelify_sans))
            ScoreFont.CUSTOM -> customFont?.let { FontFamily(Font(it)) }
                ?: FontFamily(Font(R.font.roboto_black, FontWeight.Black))
        }
    }


@Composable
internal fun appFontFamily(font: AppFont): FontFamily {
    val assets = LocalContext.current.assets
    return remember(font, assets) {
        val weights = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.Black)
        fun adwaita() = FontFamily(weights.map { weight ->
            Font(
                R.font.adwaita_sans,
                weight = weight,
                variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
            )
        })
        when (font) {
            AppFont.NUNITO -> {
                val path = "fonts/nunito.ttf"
                runCatching {
                    assets.open(path).close()
                    FontFamily(weights.map { weight ->
                        Font(path, assets, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
                    })
                }.getOrElse { adwaita() }
            }
            AppFont.SOFT_SANS -> adwaita()
            AppFont.SYSTEM -> FontFamily.Default
            AppFont.OPEN_SANS -> FontFamily(Font(R.font.open_sans), Font(R.font.open_sans_extra_bold, FontWeight.ExtraBold))
            AppFont.SANS_CODE -> FontFamily(Font(R.font.google_sans_code_bold, FontWeight.Bold))
            AppFont.HEAVY -> FontFamily(Font(R.font.roboto_black))
            AppFont.SERIF -> FontFamily(Font(R.font.noto_serif_bold, FontWeight.Bold))
        }
    }
}

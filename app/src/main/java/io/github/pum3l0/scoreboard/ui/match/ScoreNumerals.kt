package io.github.pum3l0.scoreboard.ui.match

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
internal fun ScoreNumerals(
    value: Int,
    fontFamily: FontFamily,
    sevenSegment: Boolean,
    foreground: Color,
    numberLimit: TextUnit,
    modifier: Modifier = Modifier,
) {
    val digits = value.toString()
    if (sevenSegment) {
        Canvas(modifier.semantics { text = AnnotatedString(digits) }) {
            val height = minOf(size.height * 0.86f, size.width / (digits.length * 0.70f), numberLimit.toPx())
            if (height <= 0f) return@Canvas
            val cell = height * 0.70f
            val stroke = height * 0.093f
            val originX = (size.width - cell * digits.length) / 2f
            val originY = (size.height - height) / 2f
            digits.forEachIndexed { index, digit ->
                val mask = digitSegments[digit.digitToInt()]
                val x = originX + cell * index
                fun point(horizontal: Float, vertical: Float) = Offset(x + height * horizontal, originY + height * vertical)
                fun segment(bit: Int, x1: Float, y1: Float, x2: Float, y2: Float) {
                    if (mask and (1 shl bit) != 0) {
                        drawLine(foreground, point(x1, y1), point(x2, y2), stroke, StrokeCap.Round)
                    }
                }
                segment(0, 0.20f, 0.07f, 0.50f, 0.07f)
                segment(1, 0.57f, 0.15f, 0.57f, 0.42f)
                segment(2, 0.57f, 0.58f, 0.57f, 0.85f)
                segment(3, 0.20f, 0.93f, 0.50f, 0.93f)
                segment(4, 0.13f, 0.58f, 0.13f, 0.85f)
                segment(5, 0.13f, 0.15f, 0.13f, 0.42f)
                segment(6, 0.20f, 0.50f, 0.50f, 0.50f)
            }
        }
    } else {
        Text(
            digits,
            modifier = modifier.fillMaxWidth(),
            style = MaterialTheme.typography.displayLargeEmphasized.copy(
                fontFamily = fontFamily,
                fontSynthesis = FontSynthesis.None,
                lineHeight = TextUnit.Unspecified,
            ),
            color = foreground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = numberLimit, stepSize = 2.sp),
        )
    }
}

private val digitSegments = intArrayOf(0x3f, 0x06, 0x5b, 0x4f, 0x66, 0x6d, 0x7d, 0x07, 0x7f, 0x6f)

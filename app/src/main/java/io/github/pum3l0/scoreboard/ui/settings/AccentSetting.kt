package io.github.pum3l0.scoreboard.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.pum3l0.scoreboard.R
import io.github.pum3l0.scoreboard.ui.theme.contrastingText

@Composable
internal fun AccentSetting(
    title: Int,
    selected: Int?,
    fallback: Color,
    recentColors: List<Int>,
    onChange: (Int?) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val color = selected?.let { Color(it) } ?: fallback
    SegmentedListItem(
        onClick = { open = true },
        shapes = ListItemDefaults.segmentedShapes(0, 1),
        supportingContent = { Text(selected?.let(::toHex) ?: stringResource(R.string.accent_inherited)) },
        trailingContent = {
            Box(Modifier.size(30.dp).background(color, CircleShape))
        },
    ) { Text(stringResource(title)) }

    if (open) {
        AccentPicker(
            title = stringResource(title),
            initial = color.toArgb(),
            recentColors = recentColors,
            onSave = {
                onChange(it)
                open = false
            },
            onReset = {
                onChange(null)
                open = false
            },
            onDismiss = { open = false },
        )
    }
}

@Composable
private fun AccentPicker(
    title: String,
    initial: Int,
    recentColors: List<Int>,
    onSave: (Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(toHex(initial)) }
    val color = parseHex(value)
    var hsv by rememberSaveable { mutableStateOf(toHsv(initial)) }
    var manual by rememberSaveable { mutableStateOf(false) }
    fun updateVisual(next: FloatArray) {
        hsv = next
        value = toHex(Color.hsv(next[0], next[1], next[2]).toArgb())
    }
    fun updateExact(next: String) {
        value = next
        parseHex(next)?.let { hsv = toHsv(it) }
    }
    val preview = color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = preview,
                    contentColor = preview.contrastingText(MaterialTheme.colorScheme.onPrimary),
                ) {
                    Text(stringResource(R.string.accent_sample), Modifier.padding(12.dp))
                }
                ColorPlane(hsv) { saturation, brightness ->
                    updateVisual(floatArrayOf(hsv[0], saturation, brightness))
                }
                HueSlider(hsv[0]) { updateVisual(floatArrayOf(it, hsv[1], hsv[2])) }
                if (recentColors.isNotEmpty()) {
                    Text(stringResource(R.string.recent_colors), style = MaterialTheme.typography.labelLarge)
                    ColorSwatches(recentColors, color) { updateExact(toHex(it)) }
                }
                TextButton(onClick = { manual = !manual }, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.accent_manual))
                }
                if (manual) {
                    ColorSwatches(colors, color) { updateExact(toHex(it)) }
                    OutlinedTextField(
                        value = value,
                        onValueChange = { updateExact(it.take(7)) },
                        singleLine = true,
                        label = { Text(stringResource(R.string.accent_hex)) },
                        isError = color == null,
                        supportingText = {
                            Text(stringResource(if (color == null) R.string.accent_hex_error else R.string.accent_adapts))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                TextButton(onClick = onReset, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.accent_reset))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { color?.let(onSave) },
                enabled = color != null,
                shapes = ButtonDefaults.shapes(),
            ) { Text(stringResource(R.string.save_accent)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun ColorSwatches(swatches: List<Int>, selected: Int?, onSelect: (Int) -> Unit) {
    FlowRow(
        Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        swatches.forEach { swatch ->
            val color = Color(swatch)
            Box(
                Modifier.size(48.dp)
                    .semantics { contentDescription = toHex(swatch) }
                    .selectable(selected = selected == swatch, role = Role.RadioButton) { onSelect(swatch) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(color),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected == swatch) Text("✓", color = color.contrastingText(Color.White))
                }
            }
        }
    }
}

@Composable
private fun ColorPlane(hsv: FloatArray, onChange: (Float, Float) -> Unit) {
    val update by rememberUpdatedState(onChange)
    val saturationLabel = stringResource(R.string.accent_saturation)
    val brightnessLabel = stringResource(R.string.accent_brightness)
    val adjust = stringResource(R.string.accent_adjust, saturationLabel)
    val reduce = stringResource(R.string.accent_reduce, saturationLabel)
    val brighten = stringResource(R.string.accent_adjust, brightnessLabel)
    val darken = stringResource(R.string.accent_reduce, brightnessLabel)
    Canvas(
        Modifier.fillMaxWidth().height(144.dp).clip(MaterialTheme.shapes.large)
            .semantics {
                contentDescription = "$saturationLabel, $brightnessLabel"
                stateDescription = "${(hsv[1] * 100).toInt()}%, ${(hsv[2] * 100).toInt()}%"
                customActions = listOf(
                    CustomAccessibilityAction(adjust) {
                        onChange((hsv[1] + 0.05f).coerceAtMost(1f), hsv[2])
                        true
                    },
                    CustomAccessibilityAction(reduce) {
                        onChange((hsv[1] - 0.05f).coerceAtLeast(0f), hsv[2])
                        true
                    },
                    CustomAccessibilityAction(brighten) {
                        onChange(hsv[1], (hsv[2] + 0.05f).coerceAtMost(1f))
                        true
                    },
                    CustomAccessibilityAction(darken) {
                        onChange(hsv[1], (hsv[2] - 0.05f).coerceAtLeast(0f))
                        true
                    },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    fun select(position: Offset) {
                        update(
                            (position.x / size.width).coerceIn(0f, 1f),
                            (1f - position.y / size.height).coerceIn(0f, 1f),
                        )
                    }
                    select(down.position)
                    down.consume()
                    do {
                        val pointer = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!pointer.pressed) break
                        select(pointer.position)
                        pointer.consume()
                    } while (true)
                }
            },
    ) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hsv[0], 1f, 1f))))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val radius = 8.dp.toPx()
        val center = Offset(
            radius + (size.width - 2 * radius) * hsv[1],
            radius + (size.height - 2 * radius) * (1f - hsv[2]),
        )
        drawCircle(Color.Black, radius, center)
        drawCircle(Color.White, radius - 1.dp.toPx(), center)
        drawCircle(Color.hsv(hsv[0], hsv[1], hsv[2]), radius - 3.dp.toPx(), center)
    }
}

@Composable
private fun HueSlider(hue: Float, onChange: (Float) -> Unit) {
    val slider = remember { SliderState(hue, 0, 0f..360f) }
    val label = stringResource(R.string.accent_hue)
    SideEffect { slider.value = hue }

    Text(label, style = MaterialTheme.typography.labelLarge)
    Slider(
        state = slider,
        onValueChange = {
            slider.value = it
            onChange(it)
        },
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
        track = {
            Box(
                Modifier.fillMaxWidth().height(12.dp).background(
                    Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f, 1f, 1f) }),
                    CircleShape,
                ),
            )
        },
    )
}

private fun toHsv(argb: Int) = FloatArray(3).also { android.graphics.Color.colorToHSV(argb, it) }

private val colors = listOf(0xff7656b3, 0xff266dbc, 0xff007e72, 0xff467b40, 0xffb85720, 0xffaf4565).map(Long::toInt)

internal fun parseHex(value: String): Int? {
    val hex = value.trim().removePrefix("#")
    if (hex.length != 6 || hex.any { it !in "0123456789abcdefABCDEF" }) return null
    return hex.toIntOrNull(16)?.let { it or 0xff000000.toInt() }
}

private fun toHex(argb: Int): String = "#" + (argb and 0x00ffffff).toString(16).padStart(6, '0').uppercase()

package io.github.pum3l0.scoreboard.ui.components

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun PredictiveBackFrame(
    enabled: Boolean,
    screenKey: Any,
    onBack: () -> Unit,
    preview: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val commit = remember { Animatable(0f) }
    val currentKey = rememberUpdatedState(screenKey)
    var committingFrom by remember { mutableStateOf<Any?>(null) }
    val scope = rememberCoroutineScope()
    val motion = MaterialTheme.motionScheme
    var swipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    var verticalDrag by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler(enabled = enabled) { events ->
        var startY: Float? = null
        try {
            progress.stop()
            events.collect { event ->
                swipeEdge = event.swipeEdge
                if (startY == null) startY = event.touchY
                verticalDrag = event.touchY - checkNotNull(startY)
                progress.snapTo(event.progress.coerceIn(0f, 1f))
            }
            if (progress.value > 0f) {
                val source = currentKey.value
                committingFrom = source
                commit.animateTo(1f, motion.fastEffectsSpec())
                onBack()
                withTimeoutOrNull(400) { snapshotFlow { currentKey.value }.first { it != source } }
            } else {
                onBack()
            }
            progress.snapTo(0f)
            commit.snapTo(0f)
            committingFrom = null
        } catch (cancelled: CancellationException) {
            scope.launch {
                commit.snapTo(0f)
                committingFrom = null
                progress.animateTo(0f, motion.fastSpatialSpec())
            }
            throw cancelled
        }
    }

    val showingGesture = committingFrom == null || committingFrom == screenKey
    Box(Modifier.fillMaxSize()) {
        if (enabled && showingGesture && progress.value > 0f) {
            Box(
                Modifier.fillMaxSize().clearAndSetSemantics {}
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                            }
                        }
                    },
            ) { preview() }
        }
        Surface(
            Modifier.fillMaxSize().graphicsLayer {
                val fraction = if (showingGesture) progress.value else 0f
                alpha = if (showingGesture) 1f - commit.value.coerceIn(0f, 1f) else 1f
                val edge = if (swipeEdge == BackEventCompat.EDGE_LEFT) 1 else -1
                scaleX = 1f - 0.10f * fraction
                scaleY = scaleX
                translationX = 38.dp.toPx() * fraction * edge
                translationY = (verticalDrag * 0.14f).coerceIn(-12.dp.toPx(), 12.dp.toPx()) * fraction
                shape = RoundedCornerShape(28.dp * fraction)
                clip = fraction > 0f
                shadowElevation = 8.dp.toPx() * fraction
            },
            color = MaterialTheme.colorScheme.surface,
        ) { content() }
    }
}

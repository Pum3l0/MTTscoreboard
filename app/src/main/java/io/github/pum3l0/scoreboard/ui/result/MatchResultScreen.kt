package io.github.pum3l0.scoreboard.ui.result

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.pum3l0.scoreboard.R
import io.github.pum3l0.scoreboard.data.match.MatchTiming
import io.github.pum3l0.scoreboard.domain.match.MatchState
import io.github.pum3l0.scoreboard.domain.match.PlayerSide
import io.github.pum3l0.scoreboard.ui.components.AccentButton
import io.github.pum3l0.scoreboard.ui.components.AdaptiveContent
import io.github.pum3l0.scoreboard.ui.match.elapsedTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MatchResultScreen(
    state: MatchState,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onUndo: (() -> Unit)? = null,
    timing: MatchTiming? = null,
    fontFamily: FontFamily = FontFamily.Default,
) {
    val firstServer = if (state.config.firstServer == PlayerSide.FIRST) state.config.firstPlayerName else state.config.secondPlayerName
    val winner = when (checkNotNull(state.winner)) {
        PlayerSide.FIRST -> state.config.firstPlayerName
        PlayerSide.SECOND -> state.config.secondPlayerName
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val shareColors = ShareColors(
        colors.surface.toArgb(), colors.surfaceContainerHigh.toArgb(), colors.onSurface.toArgb(),
        colors.primaryContainer.toArgb(), colors.onPrimaryContainer.toArgb(),
    )
    val shareLabel = stringResource(R.string.share_result)
    val shareError = stringResource(R.string.share_failed)
    var sharing by remember { mutableStateOf(false) }

    AdaptiveContent(
        modifier = modifier,
        first = {
            Box(
                modifier = Modifier.size(72.dp).background(
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialShapes.Sunny.toShape(),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_trophy),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Text(
                stringResource(R.string.match_result),
                style = MaterialTheme.typography.headlineLargeEmphasized,
            )
            Text(
                stringResource(R.string.match_winner, winner),
                style = MaterialTheme.typography.titleLargeEmphasized,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.score_pair, state.gamesWon.first, state.gamesWon.second),
                style = MaterialTheme.typography.displayLargeEmphasized.copy(
                    fontFamily = fontFamily,
                    fontSynthesis = FontSynthesis.None,
                ),
            )
            Text(
                stringResource(R.string.player_order, state.config.firstPlayerName, state.config.secondPlayerName),
                textAlign = TextAlign.Center,
            )
            timing?.let {
                Text(stringResource(R.string.match_duration, elapsedTime(it.totalMillis)), style = MaterialTheme.typography.labelLarge)
            }
        },
        second = {
            Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLargeIncreased) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.first_server_name, firstServer), style = MaterialTheme.typography.titleMediumEmphasized)
                    state.completedGames.forEachIndexed { index, game ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(stringResource(R.string.game_number, index + 1))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(stringResource(R.string.score_pair, game.score.first, game.score.second))
                                timing?.gamesMillis?.getOrNull(index)?.let {
                                    Text(elapsedTime(it), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
            AccentButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (onUndo != null) R.string.save_result else R.string.back))
            }
            FilledTonalButton(
                enabled = !sharing,
                modifier = Modifier.fillMaxWidth(),
                shapes = ButtonDefaults.shapes(),
                onClick = {
                    sharing = true
                    scope.launch {
                        try {
                            val intent = withContext(Dispatchers.IO) { createResultShare(context, state, timing, shareColors) }
                            context.startActivity(Intent.createChooser(intent, shareLabel))
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            Toast.makeText(context, shareError, Toast.LENGTH_SHORT).show()
                        } finally {
                            sharing = false
                        }
                    }
                },
            ) { Text(shareLabel) }
            if (onUndo != null) {
                TextButton(onClick = onUndo, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.undo_point)) }
            }
        },
    )
}

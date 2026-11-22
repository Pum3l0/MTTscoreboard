package io.github.pum3l0.scoreboard.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.pum3l0.scoreboard.R
import io.github.pum3l0.scoreboard.data.match.StoredMatch
import io.github.pum3l0.scoreboard.domain.match.PlayerSide
import io.github.pum3l0.scoreboard.ui.components.AccentButton
import io.github.pum3l0.scoreboard.ui.match.MatchUiState
import io.github.pum3l0.scoreboard.ui.match.elapsedTime
import androidx.compose.ui.platform.LocalConfiguration
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryScreen(
    state: MatchUiState.History,
    onSelect: (StoredMatch) -> Unit,
    onRetry: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClear: () -> Unit,
    isTransferring: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(288.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(stringResource(R.string.match_history), style = MaterialTheme.typography.headlineLargeEmphasized)
        }
        val matches = state.matches
        item(span = { GridItemSpan(maxLineSpan) }) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FilledTonalButton(
                    onClick = onExport,
                    enabled = !matches.isNullOrEmpty() && !isTransferring,
                    shapes = ButtonDefaults.shapes(),
                ) { Text(stringResource(R.string.export_history)) }
                FilledTonalButton(
                    onClick = onImport,
                    enabled = !isTransferring,
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(R.string.import_history))
                }
                if (!matches.isNullOrEmpty()) {
                    val colors = MaterialTheme.colorScheme
                    FilledTonalButton(
                        onClick = onClear,
                        enabled = !isTransferring,
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = colors.errorContainer,
                            contentColor = colors.onErrorContainer,
                        ),
                    ) {
                        Icon(painterResource(R.drawable.ic_trash), null, Modifier.size(18.dp))
                        Text(stringResource(R.string.clear_history), Modifier.padding(start = 6.dp))
                    }
                }
                if (isTransferring) LoadingIndicator(modifier = Modifier.size(40.dp))
            }
        }
        when {
            state.failed -> item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.history_error))
                    AccentButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                }
            }
            matches == null -> item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            }
            matches.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) { EmptyHistory() }
            else -> items(matches, key = { it.id }) { match ->
                HistoryCard(match, onClick = { onSelect(match) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyHistory() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = MaterialShapes.Cookie4Sided.toShape(),
            color = MaterialTheme.colorScheme.tertiaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_history), contentDescription = null, Modifier.size(36.dp))
            }
        }
        Text(stringResource(R.string.no_history), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun HistoryCard(item: StoredMatch, onClick: () -> Unit) {
    val match = item.match
    val firstServer = if (match.config.firstServer == PlayerSide.FIRST) match.config.firstPlayerName else match.config.secondPlayerName
    val winner = if (match.winner == PlayerSide.FIRST) match.config.firstPlayerName else match.config.secondPlayerName
    val locale = LocalConfiguration.current.locales[0]
    val date = remember(item.finishedAt, locale) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
            .format(Date(checkNotNull(item.finishedAt)))
    }
    Card(onClick = onClick, shape = MaterialTheme.shapes.extraLargeIncreased, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                stringResource(R.string.player_order, match.config.firstPlayerName, match.config.secondPlayerName),
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
            Text(
                stringResource(R.string.score_pair, match.gamesWon.first, match.gamesWon.second),
                style = MaterialTheme.typography.headlineMediumEmphasized,
            )
            Text(stringResource(R.string.match_winner, winner), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.first_server_name, firstServer), style = MaterialTheme.typography.bodyMedium)
            item.timing?.let {
                Text(stringResource(R.string.match_duration, elapsedTime(it.totalMillis)), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

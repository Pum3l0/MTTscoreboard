package io.github.pum3l0.scoreboard.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.pum3l0.scoreboard.R
import io.github.pum3l0.scoreboard.domain.match.MatchFormat
import io.github.pum3l0.scoreboard.domain.match.PlayerSide
import io.github.pum3l0.scoreboard.ui.components.AccentButton
import io.github.pum3l0.scoreboard.ui.components.AdaptiveContent
import io.github.pum3l0.scoreboard.ui.match.MatchUiState

@Composable
fun MatchSetupScreen(
    state: MatchUiState.Setup,
    onNameChanged: (PlayerSide, String) -> Unit,
    onFormatChanged: (MatchFormat) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AdaptiveContent(
        modifier = modifier.imePadding(),
        first = {
            TextButton(onClick = onBack, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.back)) }
            Text(stringResource(R.string.new_match), style = MaterialTheme.typography.headlineLargeEmphasized)
            OutlinedTextField(
                value = state.firstName,
                onValueChange = { onNameChanged(PlayerSide.FIRST, it) },
                label = { Text(stringResource(R.string.player_one)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.large,
            )
            OutlinedTextField(
                value = state.secondName,
                onValueChange = { onNameChanged(PlayerSide.SECOND, it) },
                label = { Text(stringResource(R.string.player_two)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.large,
            )
        },
        second = {
            Text(stringResource(R.string.match_format), style = MaterialTheme.typography.titleMediumEmphasized)
            Column(Modifier.selectableGroup()) {
                MatchFormat.entries.chunked(2).forEach { formats ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        formats.forEach { format ->
                            FilledTonalToggleButton(
                                checked = format == state.format,
                                onCheckedChange = { onFormatChanged(format) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    pluralStringResource(
                                        R.plurals.games_to_win, format.gamesToWin, format.gamesToWin,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
            AccentButton(
                onClick = onStart,
                enabled = state.canStart,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.to_scoreboard)) }
        },
    )
}

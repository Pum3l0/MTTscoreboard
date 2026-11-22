package io.github.pum3l0.scoreboard.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.pum3l0.scoreboard.R
import io.github.pum3l0.scoreboard.domain.match.MatchState
import io.github.pum3l0.scoreboard.domain.match.PlayerSide
import io.github.pum3l0.scoreboard.ui.components.AccentButton

@Composable
fun HomeScreen(
    onNewMatch: () -> Unit,
    onResumeMatch: () -> Unit,
    onRules: () -> Unit,
    pendingMatch: MatchState?,
    sidesSwapped: Boolean,
    scoreFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val scroll = rememberScrollState()
        val wide = maxWidth >= 520.dp
        val actions: @Composable () -> Unit = {
            if (pendingMatch == null) {
                AccentButton(onClick = onNewMatch, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                    Icon(painterResource(R.drawable.ic_play), contentDescription = null, Modifier.size(24.dp))
                    Text(
                        stringResource(R.string.start_match),
                        Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                    )
                }
            } else {
                PendingMatchCard(pendingMatch, scoreFamily, sidesSwapped)
                AccentButton(onClick = onResumeMatch, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (pendingMatch.winner == null) R.string.continue_match else R.string.see_result))
                }
                FilledTonalButton(
                    onClick = onNewMatch,
                    modifier = Modifier.fillMaxWidth(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(R.string.new_match))
                }
            }
            TextButton(
                onClick = onRules,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(painterResource(R.drawable.ic_rules), null, Modifier.size(20.dp))
                Text(stringResource(R.string.rules_link), modifier = Modifier.padding(start = 8.dp))
            }
        }

        if (wide) {
            Row(
                Modifier.widthIn(max = 840.dp).fillMaxWidth().verticalScroll(scroll).padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HomeTitle(Modifier.weight(1f))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) { actions() }
            }
        } else {
            Column(
                Modifier.widthIn(max = 480.dp).fillMaxWidth().verticalScroll(scroll)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (pendingMatch == null) 20.dp else 10.dp),
            ) {
                HomeTitle(compact = pendingMatch != null)
                actions()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeTitle(modifier: Modifier = Modifier, compact: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 16.dp),
    ) {
        Box(
            Modifier.size(if (compact) 68.dp else 104.dp)
                .background(colors.tertiaryContainer, MaterialShapes.Cookie4Sided.toShape()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_paddle),
                contentDescription = null,
                modifier = Modifier.size(if (compact) 34.dp else 52.dp),
                tint = colors.onTertiaryContainer,
            )
        }
        Text(
            text = buildAnnotatedString {
                append("Score")
                withStyle(SpanStyle(color = colors.primary)) { append("Board") }
            },
            style = if (compact) MaterialTheme.typography.headlineSmallEmphasized
                else MaterialTheme.typography.displaySmallEmphasized,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PendingMatchCard(match: MatchState, scoreFamily: FontFamily, sidesSwapped: Boolean) {
    val colors = MaterialTheme.colorScheme
    val game = match.activeGameScore ?: match.completedGames.last().score
    Surface(shape = MaterialTheme.shapes.extraLargeIncreased, color = colors.surfaceContainerHigh) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(if (match.winner == null) R.string.pending_match else R.string.pending_result),
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
            if (match.winner == null) {
                Text(
                    stringResource(R.string.game_number, match.completedGames.size + 1),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val leading = if (sidesSwapped) PlayerSide.SECOND else PlayerSide.FIRST
                listOf(leading, leading.opponent).forEach { side ->
                    val first = side == PlayerSide.FIRST
                    PendingPlayer(
                        name = if (first) match.config.firstPlayerName else match.config.secondPlayerName,
                        points = if (first) game.first else game.second,
                        games = if (first) match.gamesWon.first else match.gamesWon.second,
                        serving = match.server == side,
                        scoreFamily = scoreFamily,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingPlayer(
    name: String, points: Int, games: Int, serving: Boolean, scoreFamily: FontFamily, modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.largeIncreased,
        color = if (serving) colors.primaryContainer else colors.surface,
        contentColor = if (serving) colors.onPrimaryContainer else colors.onSurface,
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(name, style = MaterialTheme.typography.titleSmallEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(points.toString(), style = MaterialTheme.typography.headlineLargeEmphasized.copy(fontFamily = scoreFamily))
            Text(stringResource(R.string.games_count, games), style = MaterialTheme.typography.labelMedium)
            if (serving) Text(stringResource(R.string.serving), style = MaterialTheme.typography.labelMediumEmphasized)
        }
    }
}

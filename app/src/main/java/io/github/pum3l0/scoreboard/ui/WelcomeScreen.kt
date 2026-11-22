package io.github.pum3l0.scoreboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.pum3l0.scoreboard.R
import io.github.pum3l0.scoreboard.data.settings.AppLanguage
import io.github.pum3l0.scoreboard.ui.components.AccentButton
import io.github.pum3l0.scoreboard.ui.components.AdaptiveContent

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun WelcomeScreen(onLanguageSelected: (AppLanguage) -> Unit) {
    val colors = MaterialTheme.colorScheme
    AdaptiveContent(
        modifier = Modifier.safeDrawingPadding(),
        first = {
            Box(
                Modifier.size(88.dp).background(colors.tertiaryContainer, MaterialShapes.Cookie4Sided.toShape()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_paddle), null, Modifier.size(44.dp), tint = colors.onTertiaryContainer)
            }
            Text(
                stringResource(R.string.welcome_title),
                Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineLargeEmphasized,
            )
            Text(stringResource(R.string.welcome_language), color = colors.onSurfaceVariant)
        },
        second = {
            AppLanguage.entries.forEach { language ->
                AccentButton(onClick = { onLanguageSelected(language) }, modifier = Modifier.fillMaxWidth()) {
                    Text(language.displayName)
                }
            }
        },
    )
}

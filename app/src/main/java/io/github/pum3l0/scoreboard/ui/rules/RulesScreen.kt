package io.github.pum3l0.scoreboard.ui.rules

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.pum3l0.scoreboard.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RulesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    var openTopic by rememberSaveable { mutableIntStateOf(0) }
    val focus = LocalFocusManager.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val linkError = stringResource(R.string.link_error)
    val colors = MaterialTheme.colorScheme
    val matches = ruleTopics.filter { topic ->
        val searchable = listOf(stringResource(topic.title), stringResource(topic.keywords)) +
            topic.entries.flatMap { (title, body) -> listOf(stringResource(title), stringResource(body)) }
        matchesRuleQuery(query, searchable)
    }

    Box(modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                    Icon(painterResource(R.drawable.ic_back), contentDescription = stringResource(R.string.back))
                }
                Text(
                    stringResource(R.string.rules_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.rules_search)) },
                placeholder = { Text(stringResource(R.string.rules_search_example)) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }, shapes = IconButtonDefaults.shapes()) {
                            Icon(painterResource(R.drawable.ic_clear), contentDescription = stringResource(R.string.clear_search))
                        }
                    }
                },
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
            ) {
                if (matches.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.rules_no_matches),
                            Modifier.padding(vertical = 16.dp),
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                items(matches, key = { it.title }) { topic ->
                    val expanded = openTopic == topic.title || query.isNotBlank()
                    RuleSection(
                        topic = topic,
                        expanded = expanded,
                        searching = query.isNotBlank(),
                        onToggle = {
                            focus.clearFocus()
                            openTopic = if (expanded) 0 else topic.title
                        },
                    )
                }
                item {
                    TextButton(
                        shapes = ButtonDefaults.shapes(),
                        onClick = {
                            runCatching { uriHandler.openUri(ITTF_RULES) }
                                .onFailure { Toast.makeText(context, linkError, Toast.LENGTH_SHORT).show() }
                        },
                    ) {
                        Text(stringResource(R.string.rules_source))
                        Icon(painterResource(R.drawable.ic_external), null, Modifier.padding(start = 6.dp).size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RuleSection(topic: RuleTopic, expanded: Boolean, searching: Boolean, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val expandedDescription = stringResource(if (expanded) R.string.rules_expanded else R.string.rules_collapsed)
    val title: @Composable () -> Unit = {
        Text(stringResource(topic.title), style = MaterialTheme.typography.titleMediumEmphasized)
    }
    val mark: @Composable () -> Unit = {
        Box(
            Modifier.size(36.dp).background(colors.tertiaryContainer, MaterialShapes.Cookie4Sided.toShape()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_rules), null, Modifier.size(20.dp), tint = colors.onTertiaryContainer)
        }
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLargeIncreased) {
        if (searching) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                mark()
                title()
            }
        } else {
            SegmentedListItem(
                onClick = onToggle,
                modifier = Modifier.semantics { stateDescription = expandedDescription },
                shapes = ListItemDefaults.segmentedShapes(0, 1),
                leadingContent = mark,
                trailingContent = {
                    Icon(
                        painterResource(R.drawable.ic_chevron), null,
                        Modifier.size(20.dp).rotate(if (expanded) 90f else 0f),
                    )
                },
                content = title,
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(motion.fastSpatialSpec()),
            exit = shrinkVertically(motion.fastSpatialSpec()),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                topic.entries.forEach { (heading, body) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(heading), style = MaterialTheme.typography.titleSmallEmphasized)
                        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
                Text(topic.reference, style = MaterialTheme.typography.labelMedium, color = colors.primary)
            }
        }
    }
}

private const val ITTF_RULES =
    "https://documents.ittf.sport/sites/default/files/public/2026-02/2026_Statutes_v1_consolidated_clean.pdf"

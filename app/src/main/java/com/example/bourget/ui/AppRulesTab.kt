package com.example.bourget.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.bourget.R
import com.example.bourget.data.Rule
import com.example.bourget.data.RuleKind

/** The app rules tab. Header (rule count, "add apps") stays fixed above the ink rule; only rows scroll. */
@Composable
fun AppRulesTab(
    apps: AppDirectory,
    rules: List<Rule>?,
    onAddApp: () -> Unit,
    onOpenRule: (String) -> Unit,
    onEnabledChange: (Rule, Boolean) -> Unit,
    onReorder: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(start = ScreenMargin, end = 12.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecondaryText(
                pluralStringResource(R.plurals.rules_count, rules.orEmpty().size, rules.orEmpty().size),
                Modifier.weight(1f),
            )
            UnderlineButton(stringResource(R.string.app_add), onClick = onAddApp)
        }
        InkRule(Modifier.padding(horizontal = ScreenMargin))
        when {
            rules == null -> Unit
            rules.isEmpty() -> EmptyState(
                stringResource(R.string.rules_empty_title),
                stringResource(R.string.rules_empty_apps),
            ) {
                PrimaryButton(stringResource(R.string.app_add), onClick = onAddApp, modifier = Modifier.padding(top = 16.dp))
            }
            else -> ReorderableRuleList(
                kind = RuleKind.APP,
                rules = rules,
                apps = apps,
                nameOf = { apps.label(it.target) },
                onEnabledChange = onEnabledChange,
                onOpenRule = { onOpenRule(it.target) },
                onReorder = onReorder,
            )
        }
    }
}

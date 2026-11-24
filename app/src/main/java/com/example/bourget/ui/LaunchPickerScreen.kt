package com.example.bourget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.bourget.R
import com.example.bourget.data.LaunchableApp
import com.example.bourget.data.matching
import com.example.bourget.data.RuleKind

/**
 * Picks the app a rule launches. Tapping an app saves it and returns. The rule's own app is
 * left out (Bourget is never listed); apps with their own rule are marked.
 */
@Composable
fun LaunchPickerScreen(
    kind: RuleKind,
    ruleTarget: String,
    current: String?,
    apps: List<LaunchableApp>?,
    appRuleTargets: Set<String>,
    onBack: () -> Unit,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    Column(modifier) {
        BackBar(onBack)
        ScreenTitle(stringResource(R.string.launch_picker_title))
        SecondaryText(
            stringResource(if (kind == RuleKind.APP) R.string.launch_picker_desc_app else R.string.launch_picker_desc_website),
            Modifier.padding(horizontal = ScreenMargin, vertical = 8.dp),
        )
        AppSearchField(
            query = query,
            onQueryChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).padding(bottom = 8.dp),
        )
        if (apps == null) {
            SecondaryText(stringResource(R.string.apps_loading), Modifier.padding(horizontal = ScreenMargin, vertical = 16.dp))
            return@Column
        }
        val visible = apps.matching(query).filter { it.packageName != ruleTarget }
        LazyColumn(Modifier.fillMaxSize().selectableGroup()) {
            if (visible.isEmpty()) {
                item { EmptyState(stringResource(R.string.search_empty_title), stringResource(R.string.search_empty_body)) }
            }
            itemsIndexed(visible, key = { _, app -> app.packageName }) { i, app ->
                if (i > 0) RowRule()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(app.packageName == current, role = Role.RadioButton) { onPick(app.packageName) }
                        .padding(start = ScreenMargin, end = 12.dp, top = 14.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AppIcon(app, 28.dp)
                    Column(Modifier.weight(1f)) {
                        Text(app.label, style = MaterialTheme.typography.titleMedium)
                        if (app.packageName in appRuleTargets) {
                            Text(
                                stringResource(R.string.has_rule),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    RadioButton(selected = app.packageName == current, onClick = null)
                }
            }
        }
    }
}

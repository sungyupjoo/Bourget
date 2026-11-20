package com.example.bourget.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bourget.R
import com.example.bourget.data.LaunchableApp
import com.example.bourget.data.RECOMMENDED_PACKAGES
import com.example.bourget.data.Rule
import com.example.bourget.data.RuleAction
import com.example.bourget.data.RuleKind
import com.example.bourget.data.TargetAppsStore
import com.example.bourget.data.withAdded
import com.example.bourget.data.without
import kotlinx.coroutines.launch

/**
 * Adds and removes app rules. The title, search and the selected-app chips stay fixed; only the
 * list scrolls. The chip row has a constant height and the list's structure never changes on
 * toggle, so the row under the user's finger never moves. Checking adds a rule at once,
 * unchecking removes it.
 */
@Composable
fun AppPickerScreen(
    store: TargetAppsStore,
    apps: List<LaunchableApp>?,
    rules: List<Rule>,
    onDone: () -> Unit,
    showSnackbar: ShowSnackbar,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val selected = rules.map { it.target }.toSet()
    val releasedText = stringResource(R.string.rule_released, "%s")
    val recommendedReleased = stringResource(R.string.recommended_released)

    val toggle: (LaunchableApp) -> Unit = { app ->
        val index = rules.indexOfFirst { it.target == app.packageName }
        if (index < 0) {
            scope.launch { store.addRule(RuleKind.APP, app.packageName) }
        } else {
            val removed = rules[index]
            scope.launch { store.removeRule(RuleKind.APP, app.packageName) }
            // Losing a "launch another app" setting is easy to regret, so it can be undone.
            if (removed.action == RuleAction.LAUNCH) {
                showSnackbar(releasedText.format(app.label)) {
                    scope.launch { store.editRules(RuleKind.APP) { it.withAdded(removed, index) } }
                }
            }
        }
    }
    val recommended = apps.orEmpty().filter { it.packageName in RECOMMENDED_PACKAGES }
    val setAllRecommended: (Boolean) -> Unit = { select ->
        if (select) {
            scope.launch {
                store.editRules(RuleKind.APP) { current ->
                    recommended.fold(current) { acc, app -> acc.withAdded(Rule(app.packageName)) }
                }
            }
        } else {
            val before = rules
            scope.launch {
                store.editRules(RuleKind.APP) { current ->
                    recommended.fold(current) { acc, app -> acc.without(app.packageName) }
                }
            }
            showSnackbar(recommendedReleased) {
                scope.launch { store.editRules(RuleKind.APP) { before } }
            }
        }
    }

    Column(modifier) {
        BackBar(onBack = onDone) {
            UnderlineButton(stringResource(R.string.picker_done), onClick = onDone)
        }
        ScreenTitle(stringResource(R.string.picker_title))
        CompactTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.apps_search),
            trailingIcon = {
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = stringResource(R.string.apps_search_clear),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { query = "" }
                            .padding(6.dp)
                            .size(20.dp),
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenMargin, vertical = 8.dp),
        )
        // Chips follow rule order (newest first) and only show installed apps.
        val byPackage = apps.orEmpty().associateBy { it.packageName }
        SelectedChips(rules.mapNotNull { byPackage[it.target] }, onRemove = toggle)
        AppList(apps, recommended, selected, query.trim(), toggle, setAllRecommended, Modifier.weight(1f))
    }
}

@Composable
private fun SelectedChips(apps: List<LaunchableApp>, onRemove: (LaunchableApp) -> Unit) {
    Text(
        if (apps.isEmpty()) stringResource(R.string.chips_empty) else stringResource(R.string.chips_count, apps.size),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = ScreenMargin),
    )
    val listState = rememberLazyListState()
    // A newly selected app appears first; bring it into view.
    LaunchedEffect(apps.firstOrNull()?.packageName) { listState.animateScrollToItem(0) }
    // Constant height whether empty or not, so the list below never shifts.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = ScreenMargin),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(apps, key = { it.packageName }) { app ->
                InputChip(
                    selected = false,
                    onClick = { onRemove(app) },
                    label = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    shape = CircleShape,
                    avatar = {
                        Image(
                            bitmap = app.icon,
                            contentDescription = null,
                            modifier = Modifier.size(InputChipDefaults.AvatarSize),
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(R.string.chip_remove, app.label),
                            modifier = Modifier.size(InputChipDefaults.IconSize),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun AppList(
    apps: List<LaunchableApp>?,
    recommended: List<LaunchableApp>,
    selected: Set<String>,
    query: String,
    onToggle: (LaunchableApp) -> Unit,
    onSetAllRecommended: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (apps == null) {
        Text(
            stringResource(R.string.apps_loading),
            modifier = modifier.padding(horizontal = ScreenMargin, vertical = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val visible = if (query.isEmpty()) apps else apps.filter {
        it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
    }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        if (visible.isEmpty()) {
            item {
                EmptyState(stringResource(R.string.search_empty_title), stringResource(R.string.search_empty_body))
            }
        }
        // Recommended apps first, only without a search. An app in both sections toggles in both.
        if (query.isEmpty() && recommended.isNotEmpty()) {
            item(key = "recommended") {
                val allSelected = recommended.all { it.packageName in selected }
                SectionHeader(stringResource(R.string.section_recommended)) {
                    UnderlineButton(
                        stringResource(if (allSelected) R.string.deselect_all else R.string.select_all),
                        onClick = { onSetAllRecommended(!allSelected) },
                    )
                }
            }
            itemsIndexed(recommended, key = { _, app -> "recommended:${app.packageName}" }) { i, app ->
                if (i > 0) RowRule()
                CheckRow(app, checked = app.packageName in selected) { onToggle(app) }
            }
            item(key = "all") {
                SectionHeader(stringResource(R.string.section_all_apps), Modifier.padding(top = 32.dp))
            }
        }
        itemsIndexed(visible, key = { _, app -> app.packageName }) { i, app ->
            if (i > 0) RowRule()
            CheckRow(app, checked = app.packageName in selected) { onToggle(app) }
        }
    }
}

@Composable
fun CheckRow(app: LaunchableApp, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = ScreenMargin, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AppIcon(app, 28.dp)
        Text(app.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        // Row handles the click so the whole line is one touch target.
        Checkbox(checked = checked, onCheckedChange = null)
    }
}

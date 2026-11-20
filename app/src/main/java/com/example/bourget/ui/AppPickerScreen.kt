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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bourget.R
import com.example.bourget.data.LaunchableApp
import com.example.bourget.data.RuleKind
import com.example.bourget.data.TargetAppsStore
import com.example.bourget.data.loadLaunchableApps
import com.example.bourget.ui.theme.LocalInactiveTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shown while the accessibility service is on, with an "apps" and a "websites" tab. In the apps
 * tab the title, search and the selected-app chips stay fixed; only the app list scrolls. The chip row has a constant height and the list's structure
 * never changes on toggle, so the row under the user's finger never moves.
 */
@Composable
fun AppPickerScreen(store: TargetAppsStore, refreshKey: Int, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val iconSizePx = with(LocalDensity.current) { 40.dp.roundToPx() }
    var apps by remember { mutableStateOf<List<LaunchableApp>?>(null) }
    // Reloaded on every resume ([refreshKey]) so apps installed or removed while Bourget was in
    // the background are reflected. The previous list stays on screen while reloading.
    LaunchedEffect(refreshKey) {
        val loaded = withContext(Dispatchers.IO) { loadLaunchableApps(context, iconSizePx) }
        apps = loaded
        // Forget selections for apps that are gone. An empty result is treated as a failed
        // query, never as "nothing installed", so it cannot wipe the whole selection.
        if (loaded.isNotEmpty()) store.retainOnly(loaded.map { it.packageName }.toSet())
    }
    val rules by store.appRules.collectAsState(initial = emptyList())
    val selected = rules.map { it.target }.toSet()
    var query by rememberSaveable { mutableStateOf("") }
    var showInfo by rememberSaveable { mutableStateOf(false) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val toggle: (LaunchableApp) -> Unit = { app ->
        scope.launch {
            if (app.packageName in selected) store.removeRule(RuleKind.APP, app.packageName)
            else store.addRule(RuleKind.APP, app.packageName)
        }
    }

    Scaffold { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showInfo = true }) {
                    Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.info_open))
                }
            }
            Row(
                Modifier.padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 8.dp).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                TextTab(stringResource(R.string.tab_apps), selected = tab == 0) { tab = 0 }
                TextTab(stringResource(R.string.tab_websites), selected = tab == 1) { tab = 1 }
            }
            if (tab == 1) {
                WebsiteRulesTab(store, Modifier.weight(1f))
                return@Column
            }
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
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            )
            // Only installed apps; selections of removed apps are pruned when the list loads.
            val selectedApps = apps.orEmpty().filter { it.packageName in selected }
            SelectedChips(selectedApps, onRemove = toggle)
            HorizontalDivider(Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.onSurface)
            AppList(apps, selected, query.trim(), toggle, Modifier.weight(1f))
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.main_disclosure_title)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.main_disclosure_body))
                    TextButton(onClick = {
                        showInfo = false
                        showPrivacy = true
                    }) { Text(stringResource(R.string.privacy_policy_open)) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showInfo = false
                    onOpenSettings()
                }) { Text(stringResource(R.string.info_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { showInfo = false }) { Text(stringResource(R.string.info_close)) }
            },
        )
    }
    if (showPrivacy) PrivacyPolicyDialog(onDismiss = { showPrivacy = false })
}

/** A tab drawn as large type: the selected one in ink, the other faded. */
@Composable
private fun TextTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.headlineSmall,
        color = if (selected) MaterialTheme.colorScheme.onSurface else LocalInactiveTab.current,
        modifier = Modifier
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(vertical = 6.dp),
    )
}

@Composable
private fun SelectedChips(apps: List<LaunchableApp>, onRemove: (LaunchableApp) -> Unit) {
    Text(
        stringResource(R.string.section_selected, apps.size),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp),
    )
    // Constant height whether empty or not, so the list below never shifts.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (apps.isEmpty()) {
            Text(
                stringResource(R.string.section_selected_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(apps, key = { it.packageName }) { app ->
                    InputChip(
                        selected = false,
                        onClick = { onRemove(app) },
                        label = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
}

@Composable
private fun AppList(
    apps: List<LaunchableApp>?,
    selected: Set<String>,
    query: String,
    onToggle: (LaunchableApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (apps == null) {
        ListMessage(stringResource(R.string.apps_loading), modifier)
        return
    }
    val visible = if (query.isEmpty()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        if (visible.isEmpty()) item { ListMessage(stringResource(R.string.apps_empty)) }
        itemsIndexed(visible, key = { _, app -> app.packageName }) { i, app ->
            if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 24.dp))
            AppRow(app, checked = app.packageName in selected) { onToggle(app) }
        }
    }
}

@Composable
private fun AppRow(app: LaunchableApp, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = 24.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(32.dp))
        Text(app.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        // Row handles the click so the whole line is one touch target.
        Checkbox(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ListMessage(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

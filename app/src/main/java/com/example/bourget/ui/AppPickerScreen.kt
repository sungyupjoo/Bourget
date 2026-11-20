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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bourget.R
import com.example.bourget.data.LaunchableApp
import com.example.bourget.data.TargetAppsStore
import com.example.bourget.data.loadLaunchableApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shown while the accessibility service is on, with an "apps" and a "websites" tab. In the apps
 * tab the app bar, search and the selected-app chips stay fixed; only the app list scrolls. The chip row has a constant height and the list's structure
 * never changes on toggle, so the row under the user's finger never moves.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val selected by store.targetPackages.collectAsState(initial = emptySet())
    var query by rememberSaveable { mutableStateOf("") }
    var showInfo by rememberSaveable { mutableStateOf(false) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val toggle: (LaunchableApp) -> Unit = { app ->
        scope.launch { store.setSelected(app.packageName, app.packageName !in selected) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showInfo = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.info_open))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.tab_apps)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.tab_websites)) })
            }
            if (tab == 1) {
                WebsiteRulesTab(store, Modifier.weight(1f))
                return@Column
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.apps_search)) },
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.apps_search_clear))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            // Only installed apps; selections of removed apps are pruned when the list loads.
            val selectedApps = apps.orEmpty().filter { it.packageName in selected }
            SelectedChips(selectedApps, onRemove = toggle)
            HorizontalDivider()
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

@Composable
private fun SelectedChips(apps: List<LaunchableApp>, onRemove: (LaunchableApp) -> Unit) {
    Text(
        stringResource(R.string.section_selected, apps.size),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp),
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
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
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
        items(visible, key = { it.packageName }) { app ->
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
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(40.dp))
        Text(app.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
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

package com.example.bourget.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.bourget.R
import com.example.bourget.data.LaunchableApp
import com.example.bourget.data.Rule
import com.example.bourget.data.RuleKind
import com.example.bourget.data.TargetAppsStore
import com.example.bourget.data.loadLaunchableApps
import com.example.bourget.data.withAdded
import com.example.bourget.data.withLaunch
import com.example.bourget.ui.theme.LocalInactiveTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Screens inside the main area. Kept on a back stack of strings so it survives recreation. */
private sealed interface Route {
    data object Home : Route
    data object AppPicker : Route
    data class Detail(val kind: RuleKind, val target: String) : Route
    data class LaunchPicker(val kind: RuleKind, val target: String) : Route
    data object Settings : Route
    data object Template : Route
    data object Licenses : Route
}

private fun Route.encode(): String = when (this) {
    Route.Home -> "home"
    Route.AppPicker -> "picker"
    Route.Settings -> "settings"
    Route.Template -> "template"
    Route.Licenses -> "licenses"
    is Route.Detail -> "detail:${kind.name}:$target"
    is Route.LaunchPicker -> "launch:${kind.name}:$target"
}

private fun decodeRoute(text: String): Route {
    val parts = text.split(':', limit = 3)
    val kind = RuleKind.entries.firstOrNull { it.name == parts.getOrNull(1) }
    val target = parts.getOrNull(2)
    return when {
        parts[0] == "picker" -> Route.AppPicker
        parts[0] == "settings" -> Route.Settings
        parts[0] == "template" -> Route.Template
        parts[0] == "licenses" -> Route.Licenses
        kind == null || target == null -> Route.Home
        parts[0] == "detail" -> Route.Detail(kind, target)
        parts[0] == "launch" -> Route.LaunchPicker(kind, target)
        else -> Route.Home
    }
}

/** Shows [text] with an optional "실행 취소" that runs [undo]. */
typealias ShowSnackbar = (text: String, undo: (() -> Unit)?) -> Unit

/**
 * Shown while consent is given and the service is on. Loads installed apps once per resume
 * ([refreshKey]) for icons and names, and owns the back stack and snackbar for every screen
 * below it. [message] is a one-time snackbar from the activity (e.g. "Bourget 켜짐").
 */
@Composable
fun MainScreen(
    store: TargetAppsStore,
    refreshKey: Int,
    onOpenAccessibilitySettings: () -> Unit,
    message: String?,
    onMessageShown: () -> Unit,
) {
    val context = LocalContext.current
    val iconSizePx = with(LocalDensity.current) { 48.dp.roundToPx() }
    var appList by remember { mutableStateOf<List<LaunchableApp>?>(null) }
    // Reloaded on every resume so apps installed or removed while Bourget was in the background
    // are reflected. The previous list stays on screen while reloading.
    LaunchedEffect(refreshKey) {
        val loaded = withContext(Dispatchers.IO) { loadLaunchableApps(context, iconSizePx) }
        appList = loaded
        // An empty result is treated as a failed query, never as "nothing installed", so it
        // cannot wipe every rule.
        if (loaded.isNotEmpty()) store.retainOnly(loaded.map { it.packageName }.toSet())
    }
    val apps: AppDirectory = remember(appList) { appList?.associateBy { it.packageName } }
    val appRules by store.appRules.collectAsState(initial = null)
    val websiteRules by store.websiteRules.collectAsState(initial = null)
    val template by store.confirmTemplate.collectAsState(initial = null)

    var stack by rememberSaveable { mutableStateOf(listOf(Route.Home.encode())) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val navigate: (Route) -> Unit = { stack = stack + it.encode() }
    val back: () -> Unit = { stack = stack.dropLast(1) }
    BackHandler(enabled = stack.size > 1, onBack = back)

    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    val undoLabel = stringResource(R.string.undo)
    val showSnackbar: ShowSnackbar = { text, undo ->
        scope.launch {
            snackbarHost.currentSnackbarData?.dismiss()
            val result = snackbarHost.showSnackbar(
                text,
                actionLabel = if (undo != null) undoLabel else null,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) undo?.invoke()
        }
    }
    LaunchedEffect(message) {
        if (message != null) {
            showSnackbar(message, null)
            onMessageShown()
        }
    }

    val ruleDeleted = stringResource(R.string.rule_deleted)
    val templateSaved = stringResource(R.string.template_saved)
    val rulesOf: (RuleKind) -> List<Rule>? = { if (it == RuleKind.APP) appRules else websiteRules }
    val appRuleTargets = appRules.orEmpty().map { it.target }.toSet()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }) { innerPadding ->
        val modifier = Modifier.padding(innerPadding).fillMaxSize()
        when (val route = decodeRoute(stack.last())) {
            Route.Home -> HomeScreen(
                store = store,
                apps = apps,
                appRules = appRules,
                websiteRules = websiteRules,
                tab = tab,
                onTabChange = { tab = it },
                onAddApp = { navigate(Route.AppPicker) },
                onOpenRule = { kind, target -> navigate(Route.Detail(kind, target)) },
                onOpenSettings = { navigate(Route.Settings) },
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                showSnackbar = showSnackbar,
                modifier = modifier,
            )
            Route.AppPicker -> AppPickerScreen(
                store = store,
                apps = appList,
                rules = appRules.orEmpty(),
                onDone = back,
                showSnackbar = showSnackbar,
                modifier = modifier,
            )
            is Route.Detail -> {
                val rules = rulesOf(route.kind) ?: return@Scaffold
                val index = rules.indexOfFirst { it.target == route.target }
                // Gone (e.g. its app was uninstalled while we were away): nothing to show.
                if (index < 0) {
                    LaunchedEffect(Unit) { back() }
                    return@Scaffold
                }
                val rule = rules[index]
                RuleDetailScreen(
                    kind = route.kind,
                    rule = rule,
                    apps = apps,
                    appRuleTargets = appRuleTargets,
                    template = template ?: return@Scaffold,
                    onBack = back,
                    onUpdate = { transform -> scope.launch { store.updateRule(route.kind, route.target, transform) } },
                    onPickLaunchApp = { navigate(Route.LaunchPicker(route.kind, route.target)) },
                    onEditTemplate = { navigate(Route.Template) },
                    onDelete = {
                        back()
                        scope.launch { store.removeRule(route.kind, route.target) }
                        showSnackbar(ruleDeleted) {
                            scope.launch { store.editRules(route.kind) { it.withAdded(rule, index) } }
                        }
                    },
                    modifier = modifier,
                )
            }
            is Route.LaunchPicker -> LaunchPickerScreen(
                kind = route.kind,
                ruleTarget = route.target,
                current = rulesOf(route.kind)?.firstOrNull { it.target == route.target }?.launchPackage,
                apps = appList,
                appRuleTargets = appRuleTargets,
                onBack = back,
                onPick = { pkg ->
                    scope.launch { store.updateRule(route.kind, route.target) { it.withLaunch(pkg) } }
                    back()
                },
                modifier = modifier,
            )
            Route.Settings -> SettingsScreen(
                template = template ?: return@Scaffold,
                onBack = back,
                onEditTemplate = { navigate(Route.Template) },
                onOpenLicenses = { navigate(Route.Licenses) },
                modifier = modifier,
            )
            Route.Template -> TemplateEditorScreen(
                saved = template ?: return@Scaffold,
                // Examples use the user's own first rules when there are any.
                exampleApp = appRules?.firstNotNullOfOrNull { apps?.get(it.target) },
                exampleSite = websiteRules?.firstOrNull()?.target ?: "namu.wiki",
                onBack = back,
                onSave = { text ->
                    scope.launch { store.setConfirmTemplate(text) }
                    back()
                    showSnackbar(templateSaved, null)
                },
                modifier = modifier,
            )
            Route.Licenses -> LicensesScreen(onBack = back, modifier = modifier)
        }
    }
}

@Composable
private fun HomeScreen(
    store: TargetAppsStore,
    apps: AppDirectory,
    appRules: List<Rule>?,
    websiteRules: List<Rule>?,
    tab: Int,
    onTabChange: (Int) -> Unit,
    onAddApp: () -> Unit,
    onOpenRule: (RuleKind, String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    showSnackbar: ShowSnackbar,
    modifier: Modifier = Modifier,
) {
    var showInfo by rememberSaveable { mutableStateOf(false) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(start = ScreenMargin, end = 12.dp, top = 20.dp),
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
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings_open))
            }
        }
        Row(
            Modifier.padding(horizontal = ScreenMargin).padding(top = 12.dp, bottom = 4.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TextTab(stringResource(R.string.tab_apps), selected = tab == 0) { onTabChange(0) }
            TextTab(stringResource(R.string.tab_websites), selected = tab == 1) { onTabChange(1) }
        }
        if (tab == 0) {
            AppRulesTab(
                apps = apps,
                rules = appRules,
                onAddApp = onAddApp,
                onOpenRule = { onOpenRule(RuleKind.APP, it) },
                onEnabledChange = { rule, on ->
                    scope.launch { store.updateRule(RuleKind.APP, rule.target) { it.copy(enabled = on) } }
                },
                modifier = Modifier.weight(1f),
            )
        } else {
            WebsiteRulesTab(store, websiteRules, apps, showSnackbar, { onOpenRule(RuleKind.WEBSITE, it) }, Modifier.weight(1f))
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
                    onOpenAccessibilitySettings()
                }) { Text(stringResource(R.string.info_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { showInfo = false }) { Text(stringResource(R.string.info_close)) }
            },
        )
    }
    if (showPrivacy) PrivacyPolicyDialog(onDismiss = { showPrivacy = false })
}

/** A tab drawn as large type: the selected one in ink, the other in the inactive tab color. */
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

/** The rule list. Header ("규칙 N개", "앱 추가") stays fixed above the ink rule; only rows scroll. */
@Composable
private fun AppRulesTab(
    apps: AppDirectory,
    rules: List<Rule>?,
    onAddApp: () -> Unit,
    onOpenRule: (String) -> Unit,
    onEnabledChange: (Rule, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(start = ScreenMargin, end = 12.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.rules_count, rules.orEmpty().size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
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
            else -> LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(rules, key = { _, rule -> rule.target }) { i, rule ->
                    if (i > 0) RowRule()
                    RuleRow(
                        kind = RuleKind.APP,
                        rule = rule,
                        name = apps.label(rule.target),
                        apps = apps,
                        onEnabledChange = { onEnabledChange(rule, it) },
                        onClick = { onOpenRule(rule.target) },
                    )
                }
            }
        }
    }
}

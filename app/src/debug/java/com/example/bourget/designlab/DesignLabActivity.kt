package com.example.bourget.designlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.bourget.data.LaunchableApp
import com.example.bourget.data.TargetAppsStore
import com.example.bourget.data.loadLaunchableApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Debug-only design lab. Shows the redesigned main list, confirm popup and consent screen in
 * three concepts, using the apps actually installed on this device (and the apps already selected
 * in Bourget, if any). Nothing here is stored; switches and buttons only change local state.
 */
class DesignLabActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = TargetAppsStore(this)
        setContent { DesignLab(store) }
    }
}

/** Default picks when nothing is selected in Bourget yet: apps that tend to take a lot of time. */
private val RECOMMENDED = listOf(
    "com.google.android.youtube",
    "com.instagram.android",
    "com.instagram.barcelona",
    "com.twitter.android",
    "com.zhiliaoapp.musically",
    "com.reddit.frontpage",
)

private fun buildMockRules(apps: List<LaunchableApp>, selected: Set<String>): List<MockRule> {
    val byPackage = apps.associateBy { it.packageName }
    val targets = selected.mapNotNull { byPackage[it] }
        .ifEmpty { RECOMMENDED.mapNotNull { byPackage[it] } }
        .ifEmpty { apps.take(4) }
        .take(6)
    val launchTarget = byPackage["com.ichi2.anki"] ?: apps.firstOrNull { it !in targets }
    return targets.mapIndexed { i, app ->
        MockRule(app, enabled = i != 2, launchTo = if (i == 0) launchTarget else null)
    }
}

private val PAGES = listOf("목록", "확인 창", "동의")
private val SCALES = listOf(1f, 1.3f, 2f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesignLab(store: TargetAppsStore) {
    val context = LocalContext.current
    val iconPx = with(LocalDensity.current) { 56.dp.roundToPx() }
    val systemDark = isSystemInDarkTheme()
    var concept by rememberSaveable { mutableStateOf(Concept.CARDS) }
    var dark by rememberSaveable { mutableStateOf(systemDark) }
    var scale by rememberSaveable { mutableFloatStateOf(1f) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var darkApp by rememberSaveable { mutableStateOf(true) }
    var rules by remember { mutableStateOf<List<MockRule>?>(null) }

    LaunchedEffect(Unit) {
        val apps = withContext(Dispatchers.IO) { loadLaunchableApps(context, iconPx) }
        rules = buildMockRules(apps, store.targetPackages.first())
    }

    // The lab's own controls use plain Material 3 so they never look like part of a concept.
    MaterialTheme {
        Scaffold(
            topBar = {
                Column(Modifier.statusBarsPadding().padding(vertical = 4.dp)) {
                    ChipRow {
                        Concept.entries.forEach { c ->
                            FilterChip(selected = concept == c, onClick = { concept = c }, label = { Text(c.label) })
                        }
                        FilterChip(selected = dark, onClick = { dark = !dark }, label = { Text("다크") })
                    }
                    ChipRow {
                        PAGES.forEachIndexed { i, label ->
                            FilterChip(selected = page == i, onClick = { page = i }, label = { Text(label) })
                        }
                        SCALES.forEach { s ->
                            FilterChip(selected = scale == s, onClick = { scale = s }, label = { Text("${(s * 100).toInt()}%") })
                        }
                        if (page == 1) {
                            FilterChip(selected = darkApp, onClick = { darkApp = !darkApp }, label = { Text("어두운 앱 위") })
                        }
                    }
                    if (!LocalLabFontNote.isLineSeedInstalled(context)) {
                        Text(
                            "LINE Seed 없음: res/font에 line_seed_kr_regular, line_seed_kr_bold를 넣으면 적용됩니다.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    HorizontalDivider()
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                val current = rules
                LabTheme(concept, dark, scale) {
                    when {
                        current == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("설치된 앱을 불러오는 중…")
                        }
                        current.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("표시할 앱이 없습니다.")
                        }
                        page == 0 -> MainListMock(current)
                        page == 1 -> PopupMock(current.first().app, darkApp)
                        else -> ConsentMock()
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

private object LocalLabFontNote {
    @Suppress("DiscouragedApi")
    fun isLineSeedInstalled(context: android.content.Context): Boolean =
        context.resources.getIdentifier("line_seed_kr_regular", "font", context.packageName) != 0
}

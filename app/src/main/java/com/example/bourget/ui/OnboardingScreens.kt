package com.example.bourget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.bourget.R
import com.example.bourget.data.LaunchableApp
import com.example.bourget.data.RECOMMENDED_PACKAGES
import com.example.bourget.data.loadLaunchableApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** First run, before consent: what Bourget does and its principles. */
@Composable
fun IntroScreen(onStart: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Column(
            Modifier.weight(1f).padding(top = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScreenTitle(stringResource(R.string.app_name))
            Text(
                stringResource(R.string.intro_description),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = ScreenMargin),
            )
            Column(Modifier.padding(top = 16.dp)) {
                InkRule(Modifier.padding(horizontal = ScreenMargin))
                listOf(R.string.intro_principle_privacy, R.string.intro_principle_open_source, R.string.intro_principle_free)
                    .forEachIndexed { i, principle ->
                        if (i > 0) RowRule()
                        Text(
                            stringResource(principle),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 18.dp),
                        )
                    }
            }
        }
        PrimaryButton(stringResource(R.string.intro_start), onClick = onStart, modifier = Modifier.padding(ScreenMargin))
    }
}

/**
 * First run, before consent: installed recommended apps, none checked. Picked apps are saved as
 * rules on continue; the service does not read anything until consent. Skipped (onDone at once)
 * when none of them is installed.
 */
@Composable
fun RecommendScreen(onDone: (List<String>) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val iconSizePx = with(LocalDensity.current) { 48.dp.roundToPx() }
    var apps by remember { mutableStateOf<List<LaunchableApp>?>(null) }
    LaunchedEffect(Unit) {
        val installed = withContext(Dispatchers.IO) { loadLaunchableApps(context, iconSizePx) }
        val recommended = installed.filter { it.packageName in RECOMMENDED_PACKAGES }
        if (recommended.isEmpty()) onDone(emptyList()) else apps = recommended
    }
    var selected by rememberSaveable { mutableStateOf(listOf<String>()) }

    Column(modifier) {
        Column(Modifier.padding(top = 32.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenTitle(stringResource(R.string.recommend_title))
            Text(
                stringResource(R.string.recommend_description),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = ScreenMargin),
            )
        }
        val list = apps
        if (list == null) {
            SecondaryText(stringResource(R.string.apps_loading), Modifier.weight(1f).padding(horizontal = ScreenMargin))
        } else {
            val allSelected = list.all { it.packageName in selected }
            SectionHeader(pluralStringResource(R.plurals.recommend_section, list.size, list.size)) {
                UnderlineButton(
                    stringResource(if (allSelected) R.string.deselect_all else R.string.select_all),
                    onClick = { selected = if (allSelected) emptyList() else list.map { it.packageName } },
                )
            }
            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(list, key = { _, app -> app.packageName }) { i, app ->
                    if (i > 0) RowRule()
                    CheckRow(app, checked = app.packageName in selected) {
                        selected = if (app.packageName in selected) selected - app.packageName else selected + app.packageName
                    }
                }
                item {
                    Text(
                        stringResource(R.string.recommend_footer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 16.dp),
                    )
                }
            }
        }
        PrimaryButton(
            if (selected.isEmpty()) stringResource(R.string.recommend_skip)
            else stringResource(R.string.recommend_continue, selected.size),
            onClick = { onDone(selected) },
            enabled = list != null,
            modifier = Modifier.padding(ScreenMargin),
        )
    }
}

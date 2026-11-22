package com.example.bourget.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.bourget.R
import com.example.bourget.data.AppLanguage
import com.example.bourget.data.NAME_PLACEHOLDERS

/** Settings in two tabs: 일반 (language, popup text, backup) and 정보 (licenses, source, version). */
@Composable
fun SettingsScreen(
    template: String,
    backupEnabled: Boolean,
    onBackupChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onEditTemplate: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Kept across the recreation that a language change causes.
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier) {
        BackBar(onBack)
        ScreenTitle(stringResource(R.string.settings_title))
        Row(
            Modifier.padding(horizontal = ScreenMargin).padding(top = 12.dp, bottom = 4.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TextTab(stringResource(R.string.settings_tab_general), selected = tab == 0) { tab = 0 }
            TextTab(stringResource(R.string.settings_tab_about), selected = tab == 1) { tab = 1 }
        }
        InkRule(Modifier.padding(horizontal = ScreenMargin))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (tab == 0) {
                GeneralSettings(template, backupEnabled, onBackupChange, onEditTemplate)
            } else {
                AboutSettings(onOpenLicenses)
            }
        }
    }
}

@Composable
private fun GeneralSettings(
    template: String,
    backupEnabled: Boolean,
    onBackupChange: (Boolean) -> Unit,
    onEditTemplate: () -> Unit,
) {
    val activity = LocalContext.current as Activity
    var language by remember { mutableStateOf(AppLanguage.current(activity)) }
    var pickLanguage by rememberSaveable { mutableStateOf(false) }

    SettingRow(stringResource(R.string.settings_language), onClick = { pickLanguage = true }) {
        SecondaryText(stringResource(language.label()))
    }
    RowRule()
    SettingRow(stringResource(R.string.settings_template), onClick = onEditTemplate) {
        Text(
            templateWithNameChip(template),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    RowRule()
    // The switch sits beside the title only; the description runs the full width below.
    Column(
        Modifier
            .fillMaxWidth()
            .toggleable(backupEnabled, role = Role.Switch, onValueChange = onBackupChange)
            .padding(horizontal = ScreenMargin, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                stringResource(R.string.settings_backup),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = backupEnabled, onCheckedChange = null)
        }
        SecondaryText(stringResource(R.string.settings_backup_desc))
    }

    if (pickLanguage) {
        LanguageDialog(
            current = language,
            onDismiss = { pickLanguage = false },
            onPick = { picked ->
                pickLanguage = false
                if (picked != language) {
                    language = picked
                    AppLanguage.set(activity, picked)
                }
            },
        )
    }
}

@Composable
private fun AboutSettings(onOpenLicenses: () -> Unit) {
    val context = LocalContext.current
    val sourceUrl = stringResource(R.string.source_url)
    val version = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    SettingRow(stringResource(R.string.settings_licenses), onClick = onOpenLicenses)
    RowRule()
    // Opens the browser with an intent; Bourget itself needs no internet permission.
    SettingRow(stringResource(R.string.settings_source), onClick = {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sourceUrl)))
        } catch (e: ActivityNotFoundException) {
            // No browser installed: nothing to open.
        }
    }) { SecondaryText(sourceUrl) }
    RowRule()
    SettingRow(stringResource(R.string.settings_version), onClick = null) { SecondaryText(version) }
}

private fun AppLanguage.label(): Int = when (this) {
    AppLanguage.SYSTEM -> R.string.language_system
    AppLanguage.KOREAN -> R.string.language_korean
    AppLanguage.ENGLISH -> R.string.language_english
}

@Composable
private fun LanguageDialog(current: AppLanguage, onDismiss: () -> Unit, onPick: (AppLanguage) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language)) },
        text = {
            Column(Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { language ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(language == current, role = Role.RadioButton) { onPick(language) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(selected = language == current, onClick = null)
                        Text(stringResource(language.label()), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** The template with its name placeholders set off in a tinted span, like a small chip. */
@Composable
fun templateWithNameChip(template: String): AnnotatedString {
    val chip = SpanStyle(background = MaterialTheme.colorScheme.secondaryContainer, color = MaterialTheme.colorScheme.onSurface)
    return buildAnnotatedString {
        var rest = template
        while (rest.isNotEmpty()) {
            val (index, placeholder) = NAME_PLACEHOLDERS
                .map { rest.indexOf(it) to it }
                .filter { it.first >= 0 }
                .minByOrNull { it.first } ?: break
            append(rest.substring(0, index))
            withStyle(chip) { append(" $placeholder ") }
            rest = rest.substring(index + placeholder.length)
        }
        append(rest)
    }
}

@Composable
private fun SecondaryText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SettingRow(title: String, onClick: (() -> Unit)?, detail: @Composable () -> Unit = {}) {
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = ScreenMargin, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        detail()
    }
}

/** Third-party software in the app and its license notices. */
@Composable
fun LicensesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        BackBar(onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ScreenTitle(stringResource(R.string.licenses_title), Modifier.padding(bottom = 16.dp))
            LICENSES.forEachIndexed { i, (name, notice) ->
                if (i > 0) RowRule()
                Column(
                    Modifier.padding(horizontal = ScreenMargin, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    SecondaryText(notice)
                }
            }
        }
    }
}

private val LICENSES = listOf(
    // Copyright and license text as embedded in the font files (name table IDs 0 and 13).
    "LINE Seed Sans KR" to "© LY Corporation\n" +
        "This Font Software is licensed under the SIL Open Font License, Version 1.1. This Font Software is " +
        "distributed on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or " +
        "implied.\nSee the SIL Open Font License for the specific language, permissions and limitations " +
        "governing your use of this Font Software.\nhttp://scripts.sil.org/OFL",
    "Android Jetpack (AndroidX, Jetpack Compose)" to "Copyright The Android Open Source Project\n" +
        "Apache License, Version 2.0\nhttps://www.apache.org/licenses/LICENSE-2.0",
    "Kotlin, kotlinx.coroutines" to "Copyright JetBrains s.r.o. and Kotlin Programming Language contributors\n" +
        "Apache License, Version 2.0\nhttps://www.apache.org/licenses/LICENSE-2.0",
)

package com.example.bourget.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.bourget.R
import com.example.bourget.data.NAME_PLACEHOLDER

@Composable
fun SettingsScreen(
    template: String,
    onBack: () -> Unit,
    onEditTemplate: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sourceUrl = stringResource(R.string.source_url)
    val version = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    Column(modifier) {
        BackBar(onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ScreenTitle(stringResource(R.string.settings_title), Modifier.padding(bottom = 16.dp))
            SectionHeader(stringResource(R.string.settings_section_confirm))
            SettingRow(stringResource(R.string.settings_template), onClick = onEditTemplate) {
                Text(
                    templateWithNameChip(template),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SectionHeader(stringResource(R.string.settings_section_about), Modifier.padding(top = 32.dp))
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
    }
}

/** The template with "{이름}" set off in a tinted span, like a small chip. */
@Composable
fun templateWithNameChip(template: String): AnnotatedString {
    val chip = SpanStyle(background = MaterialTheme.colorScheme.secondaryContainer, color = MaterialTheme.colorScheme.onSurface)
    return buildAnnotatedString {
        val parts = template.split(NAME_PLACEHOLDER)
        parts.forEachIndexed { i, part ->
            append(part)
            if (i < parts.lastIndex) withStyle(chip) { append(" $NAME_PLACEHOLDER ") }
        }
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

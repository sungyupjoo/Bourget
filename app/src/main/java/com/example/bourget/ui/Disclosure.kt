package com.example.bourget.ui

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.StyleSpan
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bourget.R

/**
 * The in-app disclosure (Play Accessibility API / User Data policy): what the service reads and
 * what it never does. [highlightIntro] sets the first sentence, which changed in version 3, in
 * bold for users asked to agree again.
 */
@Composable
fun DisclosureText(
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    highlightIntro: Boolean = false,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.disclosure_intro),
            style = if (highlightIntro) style.copy(fontWeight = FontWeight.Bold) else style,
        )
        Text(stringResource(R.string.disclosure_reads_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Text(stringResource(R.string.disclosure_reads), style = style)
        Text(stringResource(R.string.disclosure_never_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Text(stringResource(R.string.disclosure_never), style = style)
        Text(stringResource(R.string.disclosure_outro), style = style, modifier = Modifier.padding(top = 8.dp))
    }
}

/** ⓘ: the disclosure again, with the privacy policy and accessibility settings one tap away. */
@Composable
fun InfoDialog(onDismiss: () -> Unit, onOpenPrivacy: () -> Unit, onOpenSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.main_disclosure_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                DisclosureText(style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = {
                    onDismiss()
                    onOpenPrivacy()
                }) { Text(stringResource(R.string.privacy_policy_open)) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onOpenSettings()
            }) { Text(stringResource(R.string.info_open_settings)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.info_close)) }
        },
    )
}

/** In-app copy of the privacy policy, full screen. Keep in sync with PRIVACY.md at the repository root. */
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onBack)
    Column(modifier) {
        BackBar(onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ScreenTitle(stringResource(R.string.privacy_policy_title), Modifier.padding(bottom = 16.dp))
            InkRule(Modifier.padding(horizontal = ScreenMargin))
            Text(
                boldStringResource(R.string.privacy_policy_body),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 24.dp),
            )
        }
    }
}

/** A string resource with its <b> tags kept as bold, which stringResource drops. */
@Composable
private fun boldStringResource(id: Int): AnnotatedString {
    val text = LocalContext.current.resources.getText(id)
    return buildAnnotatedString {
        append(text.toString())
        if (text is Spanned) {
            text.getSpans(0, text.length, StyleSpan::class.java)
                .filter { it.style == Typeface.BOLD }
                .forEach { addStyle(SpanStyle(fontWeight = FontWeight.Bold), text.getSpanStart(it), text.getSpanEnd(it)) }
        }
    }
}

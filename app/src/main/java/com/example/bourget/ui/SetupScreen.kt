package com.example.bourget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bourget.R
import com.example.bourget.ui.theme.BourgetTheme

/**
 * Prominent disclosure (Play Accessibility API / User Data policy). Shown while the user has not
 * agreed to the current disclosure, or has agreed but the service is still off.
 * Consent is an explicit tap; declining or leaving the screen is never treated as consent.
 */
@Composable
fun SetupScreen(
    consented: Boolean,
    serviceEnabled: Boolean,
    onAgree: () -> Unit,
    onDecline: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPrivacy by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            if (!consented && serviceEnabled) {
                // The service is on (e.g. enabled from system settings, or the disclosure changed
                // in an update) but stays idle until the user agrees here.
                Text(stringResource(R.string.consent_required_note), style = MaterialTheme.typography.bodyMedium)
            }
            Text(stringResource(R.string.main_disclosure_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.main_disclosure_body), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { showPrivacy = true }) {
                Text(stringResource(R.string.privacy_policy_open))
            }
        }
        Button(
            onClick = if (consented) onOpenSettings else onAgree,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Text(
                stringResource(
                    when {
                        consented -> R.string.info_open_settings
                        serviceEnabled -> R.string.consent_agree_continue
                        else -> R.string.main_open_settings
                    }
                )
            )
        }
        if (!consented) {
            OutlinedButton(onClick = onDecline, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.consent_decline))
            }
        }
    }

    if (showPrivacy) PrivacyPolicyDialog(onDismiss = { showPrivacy = false })
}

@Preview(showBackground = true)
@Composable
private fun SetupScreenPreview() {
    BourgetTheme {
        SetupScreen(consented = false, serviceEnabled = false, onAgree = {}, onDecline = {}, onOpenSettings = {})
    }
}

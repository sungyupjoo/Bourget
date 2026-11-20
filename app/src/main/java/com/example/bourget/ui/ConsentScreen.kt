package com.example.bourget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.bourget.R

/** Which of the four consent screens to show; decided by MainActivity on every resume. */
enum class ConsentState {
    /** Never agreed to the current disclosure. */
    FIRST,

    /** Agreed to an older disclosure and the service is on: it stays idle until agreed again. */
    RECONSENT,

    /** Agreed, but the service is off in system settings. */
    SERVICE_OFF,

    /** Tapped "동의하지 않음". Nothing is recorded; the disclosure can be reviewed again. */
    DECLINED,
}

/**
 * Prominent disclosure and consent. Consent is an explicit tap; declining or leaving the screen
 * is never treated as consent. Only the body scrolls; buttons stay at the bottom.
 */
@Composable
fun ConsentScreen(
    state: ConsentState,
    serviceEnabled: Boolean,
    ruleCount: Int,
    onAgree: () -> Unit,
    onDecline: () -> Unit,
    onReview: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    var showInfo by rememberSaveable { mutableStateOf(false) }
    if (showPrivacy) {
        PrivacyPolicyScreen(onBack = { showPrivacy = false }, modifier = modifier)
        return
    }

    Column(modifier) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 32.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (state) {
                ConsentState.FIRST, ConsentState.RECONSENT -> {
                    if (state == ConsentState.RECONSENT) {
                        Column(Modifier.padding(horizontal = ScreenMargin), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.reconsent_title), style = MaterialTheme.typography.titleMedium)
                            SecondaryBody(stringResource(R.string.reconsent_body))
                        }
                    }
                    ScreenTitle(stringResource(R.string.main_disclosure_title))
                    InkRule(Modifier.padding(horizontal = ScreenMargin))
                    DisclosureText(Modifier.padding(horizontal = ScreenMargin), highlightIntro = state == ConsentState.RECONSENT)
                    UnderlineButton(
                        stringResource(R.string.privacy_policy_open),
                        onClick = { showPrivacy = true },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
                ConsentState.SERVICE_OFF -> {
                    ScreenTitle(stringResource(R.string.service_off_title))
                    InkRule(Modifier.padding(horizontal = ScreenMargin))
                    Body(stringResource(R.string.service_off_body))
                    Text(
                        boldStringResource(R.string.settings_path),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = ScreenMargin),
                    )
                    SecondaryBody(stringResource(R.string.service_off_rules, ruleCount), Modifier.padding(horizontal = ScreenMargin))
                    UnderlineButton(
                        stringResource(R.string.review_disclosure),
                        onClick = { showInfo = true },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
                ConsentState.DECLINED -> {
                    ScreenTitle(stringResource(R.string.declined_title))
                    InkRule(Modifier.padding(horizontal = ScreenMargin))
                    Body(stringResource(R.string.declined_body))
                    if (ruleCount > 0) Body(stringResource(R.string.declined_rules))
                }
            }
        }
        Column(
            Modifier.padding(horizontal = ScreenMargin, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            when (state) {
                ConsentState.FIRST, ConsentState.RECONSENT -> {
                    // With the service already on there is nothing to turn on in settings.
                    if (!serviceEnabled) {
                        Text(
                            boldStringResource(R.string.settings_path),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    PrimaryButton(
                        stringResource(if (serviceEnabled) R.string.consent_agree_continue else R.string.main_open_settings),
                        onClick = onAgree,
                    )
                    UnderlineButton(stringResource(R.string.consent_decline), onClick = onDecline, modifier = Modifier.fillMaxWidth())
                }
                ConsentState.SERVICE_OFF -> PrimaryButton(stringResource(R.string.info_open_settings), onClick = onOpenSettings)
                ConsentState.DECLINED -> PrimaryButton(stringResource(R.string.review_disclosure), onClick = onReview)
            }
        }
    }

    if (showInfo) {
        InfoDialog(
            onDismiss = { showInfo = false },
            onOpenPrivacy = { showPrivacy = true },
            onOpenSettings = onOpenSettings,
        )
    }
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = ScreenMargin))
}

@Composable
private fun SecondaryBody(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

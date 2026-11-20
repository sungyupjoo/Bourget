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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bourget.R
import com.example.bourget.ui.theme.BourgetTheme

/**
 * Shown while the accessibility service is off: the disclosure must be visible before consent.
 * The text scrolls; the consent button stays pinned at the bottom.
 */
@Composable
fun SetupScreen(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.main_disclosure_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.main_disclosure_body), style = MaterialTheme.typography.bodyMedium)
        }
        Button(
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Text(stringResource(R.string.main_open_settings))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SetupScreenPreview() {
    BourgetTheme { SetupScreen(onOpenSettings = {}) }
}

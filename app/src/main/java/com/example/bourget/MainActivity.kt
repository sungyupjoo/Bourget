package com.example.bourget

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bourget.service.BourgetAccessibilityService
import com.example.bourget.ui.theme.BourgetTheme

class MainActivity : ComponentActivity() {

    private var serviceEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BourgetTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        serviceEnabled = serviceEnabled,
                        onOpenSettings = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        serviceEnabled = isServiceEnabled()
    }

    /**
     * Reads the user's setting rather than AccessibilityManager's enabled list: that list only
     * contains currently bound services, so it is briefly false right after an app update while
     * the system rebinds the service.
     */
    private fun isServiceEnabled(): Boolean {
        val expected = ComponentName(this, BourgetAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }
}

@Composable
fun MainScreen(serviceEnabled: Boolean, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.main_disclosure_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.main_disclosure_body), style = MaterialTheme.typography.bodyMedium)
        Text(
            stringResource(if (serviceEnabled) R.string.main_status_enabled else R.string.main_status_disabled),
            style = MaterialTheme.typography.titleSmall,
        )
        Button(onClick = onOpenSettings) {
            Text(
                stringResource(
                    if (serviceEnabled) R.string.main_open_settings_enabled else R.string.main_open_settings
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    BourgetTheme {
        MainScreen(serviceEnabled = false, onOpenSettings = {})
    }
}

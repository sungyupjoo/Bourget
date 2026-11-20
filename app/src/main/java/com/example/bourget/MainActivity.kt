package com.example.bourget

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.bourget.data.TargetAppsStore
import com.example.bourget.service.BourgetAccessibilityService
import com.example.bourget.ui.AppPickerScreen
import com.example.bourget.ui.SetupScreen
import com.example.bourget.ui.theme.BourgetTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var serviceEnabled by mutableStateOf(false)
    private var resumeCount by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = TargetAppsStore(this)
        lifecycleScope.launch { store.migrate() }
        val openSettings = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        setContent {
            BourgetTheme {
                // null until DataStore answers, so the disclosure does not flash for consented users.
                val consented by store.consentedToCurrentDisclosure.collectAsState(initial = null)
                when {
                    consented == null -> Unit
                    consented == true && serviceEnabled ->
                        AppPickerScreen(store = store, refreshKey = resumeCount, onOpenSettings = openSettings)
                    else -> Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        SetupScreen(
                            consented = consented == true,
                            serviceEnabled = serviceEnabled,
                            onAgree = {
                                lifecycleScope.launch {
                                    store.recordConsent()
                                    if (!serviceEnabled) openSettings()
                                }
                            },
                            // Declining records nothing; the service stays idle until consent.
                            onDecline = { finish() },
                            onOpenSettings = openSettings,
                            modifier = Modifier.padding(innerPadding),
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        serviceEnabled = isServiceEnabled()
        resumeCount++
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

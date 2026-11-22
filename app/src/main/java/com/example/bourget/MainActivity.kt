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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.bourget.data.DISCLOSURE_VERSION
import com.example.bourget.data.Rule
import com.example.bourget.data.RuleKind
import com.example.bourget.data.TargetAppsStore
import com.example.bourget.data.withAdded
import com.example.bourget.service.BourgetAccessibilityService
import com.example.bourget.ui.ConsentScreen
import com.example.bourget.ui.ConsentState
import com.example.bourget.ui.IntroScreen
import com.example.bourget.ui.MainScreen
import com.example.bourget.ui.RecommendScreen
import com.example.bourget.ui.theme.BourgetTheme
import kotlinx.coroutines.launch

/**
 * Picks the screen from stored state on every resume:
 * first run (intro, recommended apps) → consent → system accessibility settings → main.
 */
class MainActivity : ComponentActivity() {

    private var serviceEnabled by mutableStateOf(false)
    private var resumeCount by mutableIntStateOf(0)
    /** One-time snackbar for the main screen. */
    private var message by mutableStateOf<String?>(null)
    /** Set when the user is sent to turn the service on, so coming back can say it is on. */
    private var awaitingService by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = TargetAppsStore(this)
        lifecycleScope.launch { store.migrate() }
        val openSettings = {
            awaitingService = true
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        setContent {
            BourgetTheme {
                // null until DataStore answers, so no screen flashes before the right one.
                val consentVersion by store.consentVersion.collectAsState(initial = null)
                val introDone by store.introDone.collectAsState(initial = null)
                val appRules by store.appRules.collectAsState(initial = emptyList())
                val websiteRules by store.websiteRules.collectAsState(initial = emptyList())
                var showRecommend by rememberSaveable { mutableStateOf(false) }
                var declined by rememberSaveable { mutableStateOf(false) }
                val version = consentVersion
                val consented = version != null && version >= DISCLOSURE_VERSION

                when {
                    version == null || introDone == null -> Unit
                    consented && serviceEnabled -> MainScreen(
                        store = store,
                        refreshKey = resumeCount,
                        onOpenAccessibilitySettings = openSettings,
                        message = message,
                        onMessageShown = { message = null },
                    )
                    else -> Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        val modifier = Modifier.padding(innerPadding).fillMaxSize()
                        val consent = @Composable { state: ConsentState ->
                            ConsentScreen(
                                state = state,
                                serviceEnabled = serviceEnabled,
                                stillOff = awaitingService,
                                ruleCount = appRules.size + websiteRules.size,
                                onAgree = {
                                    lifecycleScope.launch {
                                        store.recordConsent()
                                        if (!serviceEnabled) openSettings()
                                        else message = getString(if (version > 0) R.string.service_resumed else R.string.service_on)
                                    }
                                },
                                // Declining records nothing; the service stays idle until consent.
                                onDecline = { declined = true },
                                onReview = { declined = false },
                                onOpenSettings = openSettings,
                                modifier = modifier,
                            )
                        }
                        when {
                            consented -> consent(ConsentState.SERVICE_OFF)
                            showRecommend -> RecommendScreen(
                                onDone = { picked ->
                                    // Saved as rules only; nothing is read before consent.
                                    lifecycleScope.launch {
                                        store.editRules(RuleKind.APP) { rules ->
                                            picked.fold(rules) { acc, pkg -> acc.withAdded(Rule(pkg)) }
                                        }
                                    }
                                    showRecommend = false
                                },
                                modifier = modifier,
                            )
                            introDone == false -> IntroScreen(
                                onStart = {
                                    lifecycleScope.launch { store.setIntroDone() }
                                    showRecommend = true
                                },
                                modifier = modifier,
                            )
                            declined -> consent(ConsentState.DECLINED)
                            // Agreed to an older disclosure: ask again, the service stays idle.
                            version > 0 && serviceEnabled -> consent(ConsentState.RECONSENT)
                            else -> consent(ConsentState.FIRST)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        serviceEnabled = isServiceEnabled()
        if (awaitingService && serviceEnabled) {
            awaitingService = false
            message = getString(R.string.service_on)
        }
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

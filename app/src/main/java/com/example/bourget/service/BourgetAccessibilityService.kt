package com.example.bourget.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.example.bourget.R
import com.example.bourget.action.ConfirmOverlay
import com.example.bourget.action.objectParticle
import com.example.bourget.data.TargetAppsStore
import com.example.bourget.detection.AppLaunchDetector
import com.example.bourget.detection.browser.SUPPORTED_BROWSERS
import com.example.bourget.detection.matchingRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * App rules: uses only the package name of TYPE_WINDOW_STATE_CHANGED events.
 * Website rules: in supported browsers only, reads the address bar node (by view id) and keeps
 * just its host. Page content is never read, and nothing read from the screen is stored.
 */
class BourgetAccessibilityService : AccessibilityService() {

    private val overlay = ConfirmOverlay(this)
    private var appDetector: AppLaunchDetector? = null
    /** Keyed by website rule domain, so "same site" and per-rule cooldown reuse the app logic. */
    private var webDetector: AppLaunchDetector? = null
    private var scope: CoroutineScope? = null
    private var ignoredPackages: Set<String> = emptySet()
    private var currentApp: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private val urlCheck = Runnable { checkPending = false; checkBrowserUrl() }
    private var checkPending = false
    private var pendingSince = 0L

    override fun onServiceConnected() {
        val appDetector = AppLaunchDetector(targetPackages = emptySet())
        val webDetector = AppLaunchDetector(targetPackages = emptySet())
        this.appDetector = appDetector
        this.webDetector = webDetector
        // Keep targets in sync with the user's rules while the service is connected.
        val store = TargetAppsStore(this)
        scope = MainScope().apply {
            launch { store.targetPackages.collect { appDetector.targetPackages = it } }
            launch {
                store.websiteRules.collect { rules ->
                    webDetector.targetPackages = rules.filter { it.enabled }.map { it.domain }.toSet()
                }
            }
        }
        // Windows that appear on top of an app without the user leaving it
        // (our own overlay, system UI, keyboards) must not count as an app switch.
        val imePackages = getSystemService(InputMethodManager::class.java)
            .enabledInputMethodList.map { it.packageName }
        ignoredPackages = setOf(packageName, SYSTEM_UI_PACKAGE) + imePackages
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // Pane appeared/disappeared events also use this type and can come from an app that
                // just went to the background (seen with YouTube after pressing Home). Only
                // whole-window changes (UNDEFINED) indicate an app switch.
                if (event.contentChangeTypes != AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED) return
                if (pkg in ignoredPackages) return
                onWindowChanged(pkg)
            }
            // The address bar can change without a window change (same-tab navigation).
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ->
                if (pkg == currentApp && pkg in SUPPORTED_BROWSERS) scheduleUrlCheck()
            else -> Unit // Not subscribed in accessibility_service_config.xml.
        }
    }

    private fun onWindowChanged(pkg: String) {
        val appDetector = appDetector ?: return
        val now = SystemClock.elapsedRealtime()
        val switched = pkg != currentApp
        if (switched) {
            currentApp = pkg
            // Leaving or entering an app ends the current site visit, so coming back to the
            // browser on the same site counts as a new visit (subject to the cooldown).
            webDetector?.onForegroundChanged("app:$pkg", now)
        }

        if (appDetector.onForegroundChanged(pkg, now)) {
            val label = appLabel(pkg)
            overlay.show(
                title = getString(R.string.confirm_title, label, objectParticle(label)),
                onGoBack = { performGlobalAction(GLOBAL_ACTION_HOME) },
            )
        } else if (switched && pkg !in appDetector.targetPackages) {
            // User left the target app some other way; don't leave the dialog floating.
            overlay.dismiss()
        }

        if (pkg in SUPPORTED_BROWSERS) scheduleUrlCheck()
    }

    /**
     * Debounces bursts of browser events: runs [URL_CHECK_DEBOUNCE_MS] after the last event,
     * but no later than [URL_CHECK_MAX_DELAY_MS] after the first, so a page that keeps
     * updating while it loads is still checked.
     */
    private fun scheduleUrlCheck() {
        val now = SystemClock.uptimeMillis()
        if (!checkPending) {
            checkPending = true
            pendingSince = now
        }
        handler.removeCallbacks(urlCheck)
        val delay = minOf(URL_CHECK_DEBOUNCE_MS, pendingSince + URL_CHECK_MAX_DELAY_MS - now)
        handler.postDelayed(urlCheck, delay.coerceAtLeast(0))
    }

    @Suppress("DEPRECATION") // recycle() is a no-op on API 33+, required below it.
    private fun checkBrowserUrl() {
        val webDetector = webDetector ?: return
        val browser = currentApp ?: return
        val extractor = SUPPORTED_BROWSERS[browser] ?: return
        val root = rootInActiveWindow ?: return
        // An unreadable URL changes nothing: it is never treated as leaving the site.
        val host = try {
            if (root.packageName == browser) extractor.extractHost(root) else null
        } finally {
            root.recycle()
        } ?: return

        val rule = matchingRule(host, webDetector.targetPackages)
        if (webDetector.onForegroundChanged(rule ?: "host:$host", SystemClock.elapsedRealtime())) {
            overlay.show(
                title = getString(R.string.confirm_title_website),
                message = rule,
                onGoBack = { performGlobalAction(GLOBAL_ACTION_BACK) },
            )
        }
    }

    private fun appLabel(pkg: String): String = try {
        packageManager.getApplicationInfo(pkg, 0).loadLabel(packageManager).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        pkg
    }

    override fun onInterrupt() {
        overlay.dismiss()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        overlay.dismiss()
        handler.removeCallbacks(urlCheck)
        checkPending = false
        scope?.cancel()
        scope = null
        appDetector = null
        webDetector = null
        currentApp = null
        return super.onUnbind(intent)
    }

    private companion object {
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"
        const val URL_CHECK_DEBOUNCE_MS = 300L
        const val URL_CHECK_MAX_DELAY_MS = 1_000L
    }
}

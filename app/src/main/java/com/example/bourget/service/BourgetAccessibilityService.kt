package com.example.bourget.service

import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.example.bourget.R
import com.example.bourget.action.ConfirmOverlay
import com.example.bourget.data.AppLanguage
import com.example.bourget.data.Rule
import com.example.bourget.data.RuleAction
import com.example.bourget.data.TargetAppsStore
import com.example.bourget.data.applyTemplate
import com.example.bourget.detection.AppLaunchDetector
import com.example.bourget.detection.LaunchGuard
import com.example.bourget.detection.browser.SUPPORTED_BROWSERS
import com.example.bourget.detection.matchingRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Does nothing until the user has agreed to the current in-app disclosure (the service can be
 * enabled from system settings without ever opening Bourget).
 *
 * App rules: uses only the package name of TYPE_WINDOW_STATE_CHANGED events.
 * Website rules: in supported browsers only, reads the address bar node (by view id) and keeps
 * just its host. Page content is never read, and nothing read from the screen is stored.
 *
 * A matching rule either shows the confirm popup or launches the app the user picked for it.
 * Going back from that app to where the rule fired is deliberate and does not fire it again.
 */
class BourgetAccessibilityService : AccessibilityService() {

    private val overlay = ConfirmOverlay(this)
    private val counts = DailyCounts()
    private var appDetector: AppLaunchDetector? = null
    /** Keyed by website rule domain, so "same site" and per-rule cooldown reuse the app logic. */
    private var webDetector: AppLaunchDetector? = null
    private val launchGuard = LaunchGuard()
    private var appRules: Map<String, Rule> = emptyMap()
    private var websiteRules: Map<String, Rule> = emptyMap()
    /** The user's saved popup text; null for the default in the current language. */
    private var savedTemplate: String? = null
    private var scope: CoroutineScope? = null
    private var ignoredPackages: Set<String> = emptySet()
    /** Home screen apps (also recents on most devices); passing through them is not using an app. */
    private var homePackages: Set<String> = emptySet()
    private var currentApp: String? = null
    /**
     * Whether an address was read since switching to the current app. If not, a site that matches
     * was already open when the user came back to the browser, and going back would only reach an
     * earlier page of the same site.
     */
    private var readAddressSinceSwitch = false
    /**
     * The user came back to this browser from the app a website rule launched. The site it stayed
     * on must not launch that app again, so the first address read after the switch is exempt.
     */
    private var returnedToBrowser = false
    private var consented = false

    private val handler = Handler(Looper.getMainLooper())
    private val urlCheck = Runnable { checkPending = false; checkBrowserUrl() }
    private var checkPending = false
    private var pendingSince = 0L
    private val keyboardsObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) = updateIgnoredPackages()
    }

    override fun onServiceConnected() {
        val appDetector = AppLaunchDetector(targetPackages = emptySet())
        val webDetector = AppLaunchDetector(targetPackages = emptySet())
        this.appDetector = appDetector
        this.webDetector = webDetector
        // Keep targets in sync with the user's rules while the service is connected.
        val store = TargetAppsStore(this)
        scope = MainScope().apply {
            launch {
                store.consentedToCurrentDisclosure.collect {
                    consented = it
                    if (!it) {
                        overlay.dismiss()
                        handler.removeCallbacks(urlCheck)
                        checkPending = false
                    }
                }
            }
            launch { store.savedTemplate.collect { savedTemplate = it } }
            launch {
                store.appRules.collect { rules ->
                    appRules = rules.associateBy { it.target }
                    appDetector.targetPackages = rules.filter { it.enabled }.map { it.target }.toSet()
                }
            }
            launch {
                store.websiteRules.collect { rules ->
                    websiteRules = rules.associateBy { it.target }
                    webDetector.targetPackages = rules.filter { it.enabled }.map { it.target }.toSet()
                }
            }
        }
        updateIgnoredPackages()
        // A keyboard turned on later must not count as an app switch either.
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_INPUT_METHODS), false, keyboardsObserver,
        )
    }

    /**
     * Windows that appear on top of an app without the user leaving it (our own overlay, system UI,
     * keyboards) must not count as an app switch.
     */
    private fun updateIgnoredPackages() {
        val imePackages = getSystemService(InputMethodManager::class.java)
            .enabledInputMethodList.map { it.packageName }
        ignoredPackages = setOf(packageName, SYSTEM_UI_PACKAGE) + imePackages
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        homePackages = packageManager.queryIntentActivities(home, 0)
            .map { it.activityInfo.packageName }.toSet()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!consented) return
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
        // Coming back from the app a rule launched is deliberate: don't launch it again.
        val returned = switched && launchGuard.onSwitched(pkg, pkg in homePackages)
        if (switched) {
            currentApp = pkg
            readAddressSinceSwitch = false
            returnedToBrowser = returned && pkg in SUPPORTED_BROWSERS
            // Leaving or entering an app ends the current site visit, so coming back to the
            // browser on the same site counts as a new visit (subject to the cooldown).
            webDetector?.onForegroundChanged("app:$pkg", now)
        }

        if (appDetector.onForegroundChanged(pkg, now, returned || launchGuard.isExempt(pkg, now))) {
            if (!launchInstead(appRules[pkg], source = pkg, now)) {
                showConfirm(
                    key = "app:$pkg",
                    title = applyTemplate(template(), appLabel(pkg)),
                    icon = appIcon(pkg),
                    onGoBack = { performGlobalAction(GLOBAL_ACTION_HOME) },
                )
            }
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
        val navigated = readAddressSinceSwitch
        readAddressSinceSwitch = true
        val returned = returnedToBrowser
        returnedToBrowser = false

        val rule = matchingRule(host, webDetector.targetPackages)
        val now = SystemClock.elapsedRealtime()
        if (webDetector.onForegroundChanged(rule ?: "host:$host", now, returned || launchGuard.isExempt(browser, now))) {
            // The browser stays on the site; the user can come back to it.
            if (launchInstead(rule?.let { websiteRules[it] }, source = browser, now)) return
            showConfirm(
                key = "web:${rule ?: host}",
                title = applyTemplate(template(), rule ?: host),
                icon = getDrawable(R.drawable.ic_link)?.mutate()?.apply { setTint(ConfirmOverlay.PAPER_VARIANT) },
                // Back to the site the user came from; home when there is none to go back to.
                onGoBack = { performGlobalAction(if (navigated) GLOBAL_ACTION_BACK else GLOBAL_ACTION_HOME) },
            )
        }
    }

    /** The confirm popup, with how many times [key] was confirmed and continued today. */
    private fun showConfirm(key: String, title: String, icon: Drawable?, onGoBack: () -> Unit) {
        if (overlay.isShowing) return
        val (shown, continued) = counts.onShown(key)
        overlay.show(
            title = title,
            icon = icon,
            note = if (continued == 0) strings().getString(R.string.confirm_count, shown)
            else strings().getString(R.string.confirm_count_continued, shown, continued),
            onGoBack = onGoBack,
            onContinue = { counts.onContinued(key) },
        )
    }

    /**
     * Opens the app [rule] launches instead of the popup. Returns false (show the popup) when the
     * rule shows the popup or its app is gone. The launched app is exempt from rules for a few
     * seconds so rules that launch each other cannot loop, and coming back from it to [source]
     * (the app or browser the rule fired in) does not fire the rule again.
     */
    private fun launchInstead(rule: Rule?, source: String, now: Long): Boolean {
        val target = rule?.launchPackage?.takeIf { rule.action == RuleAction.LAUNCH } ?: return false
        val intent = packageManager.getLaunchIntentForPackage(target) ?: return false
        launchGuard.onLaunched(target, source, now)
        return try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    /** Resources in the language chosen in Bourget's settings, read when needed so a change applies at once. */
    private fun strings() = AppLanguage.localized(this).resources

    private fun template(): String = savedTemplate ?: strings().getString(R.string.template_default)

    private fun appLabel(pkg: String): String = try {
        packageManager.getApplicationInfo(pkg, 0).loadLabel(packageManager).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        pkg
    }

    private fun appIcon(pkg: String) = try {
        packageManager.getApplicationIcon(pkg)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    override fun onInterrupt() {
        overlay.dismiss()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        contentResolver.unregisterContentObserver(keyboardsObserver)
        overlay.dismiss()
        handler.removeCallbacks(urlCheck)
        checkPending = false
        scope?.cancel()
        scope = null
        appDetector = null
        webDetector = null
        currentApp = null
        readAddressSinceSwitch = false
        returnedToBrowser = false
        launchGuard.clear()
        consented = false
        return super.onUnbind(intent)
    }

    private companion object {
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"
        const val URL_CHECK_DEBOUNCE_MS = 300L
        const val URL_CHECK_MAX_DELAY_MS = 1_000L
    }
}

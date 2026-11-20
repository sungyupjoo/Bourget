package com.example.bourget.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.example.bourget.action.ConfirmOverlay
import com.example.bourget.detection.AppLaunchDetector

/**
 * Receives only TYPE_WINDOW_STATE_CHANGED events and reads only their package name.
 * Window content is never retrieved (canRetrieveWindowContent=false) and nothing is stored.
 */
class BourgetAccessibilityService : AccessibilityService() {

    private val overlay = ConfirmOverlay(this)
    private var detector: AppLaunchDetector? = null
    private var ignoredPackages: Set<String> = emptySet()

    override fun onServiceConnected() {
        // Step 1: hard-coded YouTube rule. Replaced by user rules in a later step.
        detector = AppLaunchDetector(targetPackages = setOf(YOUTUBE_PACKAGE))
        // Windows that appear on top of an app without the user leaving it
        // (our own overlay, system UI, keyboards) must not count as an app switch.
        val imePackages = getSystemService(InputMethodManager::class.java)
            .enabledInputMethodList.map { it.packageName }
        ignoredPackages = setOf(packageName, SYSTEM_UI_PACKAGE) + imePackages
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        // Pane appeared/disappeared events also use this type and can come from an app that
        // just went to the background (seen with YouTube after pressing Home). Only whole-window
        // changes (UNDEFINED) indicate an app switch.
        if (event.contentChangeTypes != AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED) return
        val detector = detector ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg in ignoredPackages) return

        if (detector.onForegroundChanged(pkg, SystemClock.elapsedRealtime())) {
            overlay.show(onGoBack = { performGlobalAction(GLOBAL_ACTION_HOME) })
        } else if (pkg !in detector.targetPackages) {
            // User left the target app some other way; don't leave the dialog floating.
            overlay.dismiss()
        }
    }

    override fun onInterrupt() {
        overlay.dismiss()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        overlay.dismiss()
        detector = null
        return super.onUnbind(intent)
    }

    private companion object {
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }
}

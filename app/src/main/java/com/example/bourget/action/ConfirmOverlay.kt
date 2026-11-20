package com.example.bourget.action

import android.accessibilityservice.AccessibilityService
import android.app.AlertDialog
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.WindowManager
import com.example.bourget.R

/**
 * Shows a standard AlertDialog in an accessibility overlay window.
 * TYPE_ACCESSIBILITY_OVERLAY needs no SYSTEM_ALERT_WINDOW permission and is not
 * subject to background activity launch restrictions.
 */
class ConfirmOverlay(private val service: AccessibilityService) {

    private var dialog: AlertDialog? = null

    fun show(appLabel: String, onGoBack: () -> Unit) {
        if (dialog?.isShowing == true) return

        var goBackChosen = false
        val themed = ContextThemeWrapper(service, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
        dialog = AlertDialog.Builder(themed)
            .setTitle(service.getString(R.string.confirm_title, appLabel, objectParticle(appLabel)))
            .setPositiveButton(R.string.confirm_continue, null)
            .setNegativeButton(R.string.confirm_go_back) { _, _ -> goBackChosen = true }
            .setCancelable(false)
            .create()
            .apply {
                window?.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
                setOnDismissListener {
                    dialog = null
                    // Run only after the dialog window is gone: AlertDialog dismisses after the
                    // click callback, and a global BACK sent before that is consumed by the dialog.
                    if (goBackChosen) onGoBack()
                }
                show()
                // Some OEM themes (e.g. Samsung One UI) bottom-align dialogs; set after show()
                // because the theme applies its gravity while the dialog is being set up.
                window?.setGravity(Gravity.CENTER)
            }
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }
}

/**
 * Korean object particle for [word]: "을" after a final consonant, "를" otherwise.
 * Falls back to "을(를)" when the word does not end in a Hangul syllable (e.g. "YouTube").
 */
internal fun objectParticle(word: String): String {
    val last = word.trimEnd().lastOrNull()
    if (last == null || last !in '가'..'힣') return "을(를)"
    return if ((last - '가') % 28 != 0) "을" else "를"
}

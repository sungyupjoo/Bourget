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

    fun show(onGoBack: () -> Unit) {
        if (dialog?.isShowing == true) return

        val themed = ContextThemeWrapper(service, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
        dialog = AlertDialog.Builder(themed)
            .setTitle(R.string.confirm_title)
            .setPositiveButton(R.string.confirm_continue, null)
            .setNegativeButton(R.string.confirm_go_back) { _, _ -> onGoBack() }
            .setCancelable(false)
            .create()
            .apply {
                window?.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
                setOnDismissListener { dialog = null }
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

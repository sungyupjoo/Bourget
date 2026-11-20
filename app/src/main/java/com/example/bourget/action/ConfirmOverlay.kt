package com.example.bourget.action

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.Paint
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.TextViewCompat
import com.example.bourget.R

/**
 * A full-screen pause over the app being opened: the app underneath is almost covered and the
 * question is set large, with "go back" as the main button. Shown in a TYPE_ACCESSIBILITY_OVERLAY
 * window, which needs no SYSTEM_ALERT_WINDOW permission and is not subject to background activity
 * launch restrictions.
 */
class ConfirmOverlay(private val service: AccessibilityService) {

    private var view: View? = null

    fun show(title: String, message: String? = null, icon: Drawable? = null, onGoBack: () -> Unit) {
        if (view != null) return
        val root = build(title, message, icon) { goBack ->
            dismiss()
            // Run only after the overlay window is gone: a global BACK sent while it is still
            // showing would be consumed by the overlay.
            if (goBack) onGoBack()
        }
        val params = WindowManager.LayoutParams(
            MATCH_PARENT,
            MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )
        service.getSystemService(WindowManager::class.java).addView(root, params)
        view = root
    }

    fun dismiss() {
        val current = view ?: return
        view = null
        service.getSystemService(WindowManager::class.java).removeViewImmediate(current)
    }

    private fun build(title: String, message: String?, icon: Drawable?, onChoice: (goBack: Boolean) -> Unit): View {
        val context: Context = service
        val bold = ResourcesCompat.getFont(context, R.font.line_seed_kr_bold)
        val regular = ResourcesCompat.getFont(context, R.font.line_seed_kr_regular)

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM
            setPadding(dp(32), dp(40), dp(32), dp(40))
        }
        if (icon != null) {
            content.addView(ImageView(context).apply { setImageDrawable(icon) }, LinearLayout.LayoutParams(dp(36), dp(36)))
        }
        content.addView(TextView(context).apply {
            text = title
            typeface = bold
            setTextColor(PAPER)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
            TextViewCompat.setLineHeight(this, sp(40))
            setPadding(0, dp(20), 0, if (message == null) dp(40) else dp(12))
        })
        if (message != null) {
            content.addView(TextView(context).apply {
                text = message
                typeface = regular
                setTextColor(PAPER_VARIANT)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setPadding(0, 0, 0, dp(40))
            })
        }
        content.addView(button(context, R.string.confirm_go_back, filled = true) { onChoice(true) },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        content.addView(button(context, R.string.confirm_continue, filled = false) { onChoice(false) },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(4) })

        // Not cancelable: BACK neither closes the pause nor reaches the app underneath.
        return object : FrameLayout(context) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean =
                event.keyCode == KeyEvent.KEYCODE_BACK || super.dispatchKeyEvent(event)
        }.apply {
            setBackgroundColor(SCRIM)
            // Keeps the buttons clear of the navigation bar.
            fitsSystemWindows = true
            isFocusable = true
            isFocusableInTouchMode = true
            addView(content, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }
    }

    private fun button(context: Context, text: Int, filled: Boolean, onClick: () -> Unit) = Button(context).apply {
        setText(text)
        isAllCaps = false
        stateListAnimator = null
        typeface = ResourcesCompat.getFont(context, R.font.line_seed_kr_bold)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        minHeight = dp(if (filled) 56 else 48)
        minimumHeight = minHeight
        val shape = GradientDrawable().apply {
            cornerRadius = dp(28).toFloat()
            setColor(if (filled) PAPER else Color.WHITE)
        }
        if (filled) {
            setTextColor(INK)
            background = RippleDrawable(ColorStateList.valueOf(0x1F000000), shape, null)
        } else {
            setTextColor(PAPER_VARIANT)
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            background = RippleDrawable(ColorStateList.valueOf(0x1FFFFFFF), null, shape)
        }
        setOnClickListener { onClick() }
    }

    private fun dp(value: Int) = (value * service.resources.displayMetrics.density).toInt()

    private fun sp(value: Int) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value.toFloat(), service.resources.displayMetrics).toInt()

    private companion object {
        val SCRIM = 0xF2141312.toInt()
        val INK = 0xFF141312.toInt()
        val PAPER = 0xFFF4F1EC.toInt()
        val PAPER_VARIANT = 0xFFBDB7AE.toInt()
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

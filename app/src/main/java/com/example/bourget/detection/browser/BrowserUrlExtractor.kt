package com.example.bourget.detection.browser

import android.view.accessibility.AccessibilityNodeInfo
import com.example.bourget.detection.hostFromAddressBar

/**
 * Reads the host currently shown in one browser's address bar.
 * Implementations look up only the address bar node and never walk page content.
 */
interface BrowserUrlExtractor {
    /** Host in the address bar of [root], or null if it cannot be read reliably. */
    fun extractHost(root: AccessibilityNodeInfo): String?
}

/** Finds the address bar by its view id. Node ids were checked on real devices (see map below). */
class AddressBarExtractor(private val viewId: String) : BrowserUrlExtractor {

    @Suppress("DEPRECATION") // recycle() is a no-op on API 33+, required below it.
    override fun extractHost(root: AccessibilityNodeInfo): String? {
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        try {
            val bar = nodes.singleOrNull() ?: return null
            // While the user is typing, the bar holds their input, not the page URL.
            if (bar.isFocused) return null
            return hostFromAddressBar(bar.text)
        } finally {
            nodes.forEach { it.recycle() }
        }
    }
}

/**
 * Browsers whose address bar was verified on a Galaxy S22 Ultra (Android 16).
 * Browsers not listed here are never inspected; Bourget does not guess URLs.
 */
val SUPPORTED_BROWSERS: Map<String, BrowserUrlExtractor> = mapOf(
    // Chrome 158: "namu.wiki/w/..." (no scheme).
    "com.android.chrome" to AddressBarExtractor("com.android.chrome:id/url_bar"),
    // Samsung Internet 31: "‎namu.wiki" (host only).
    "com.sec.android.app.sbrowser" to AddressBarExtractor("com.sec.android.app.sbrowser:id/location_bar_edit_text"),
)

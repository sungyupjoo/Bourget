package com.example.bourget.detection

/**
 * Keeps rules from firing again because of an app Bourget itself launched. Memory only; nothing
 * is stored. Pure Kotlin so it can be unit tested.
 *
 * - The launched app is exempt from rules for [windowMillis], so two apps whose rules launch
 *   each other cannot keep opening one another.
 * - Going back from the launched app to the app (or browser) it was launched from is a
 *   deliberate choice, so that one return is exempt too. It counts as a return only when the
 *   user reached the launched app and then came back without using another app in between;
 *   the home screen and recents (neutral packages) may be passed through.
 */
class LaunchGuard(private val windowMillis: Long = DEFAULT_WINDOW_MILLIS) {

    private val launchedAt = mutableMapOf<String, Long>()

    private class PendingReturn(val launched: String, val source: String) {
        var reachedLaunched = false
    }

    private var pendingReturn: PendingReturn? = null

    /** Bourget opened [packageName] because of a rule on [sourcePackage]. */
    fun onLaunched(packageName: String, sourcePackage: String, nowMillis: Long) {
        launchedAt[packageName] = nowMillis
        pendingReturn = PendingReturn(launched = packageName, source = sourcePackage)
    }

    fun isExempt(packageName: String, nowMillis: Long): Boolean {
        val at = launchedAt[packageName] ?: return false
        if (nowMillis - at < windowMillis) return true
        launchedAt.remove(packageName)
        return false
    }

    /**
     * Call on every switch to another app. Returns true when the switch is the user coming back
     * from the launched app to its source, which must not trigger the source's rule.
     * [neutral] is true for the home screen / recents.
     */
    fun onSwitched(packageName: String, neutral: Boolean): Boolean {
        val pending = pendingReturn ?: return false
        when {
            packageName == pending.launched -> pending.reachedLaunched = true
            neutral -> Unit
            packageName == pending.source && pending.reachedLaunched -> {
                pendingReturn = null
                return true
            }
            else -> pendingReturn = null
        }
        return false
    }

    fun clear() {
        launchedAt.clear()
        pendingReturn = null
    }

    companion object {
        const val DEFAULT_WINDOW_MILLIS = 5_000L
    }
}

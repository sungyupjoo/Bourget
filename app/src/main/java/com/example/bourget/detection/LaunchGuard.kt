package com.example.bourget.detection

/**
 * Remembers apps Bourget itself just launched for a rule, so they are exempt from rules for
 * [windowMillis]. Without it, two apps whose rules launch each other would keep opening one
 * another. Memory only; nothing is stored. Pure Kotlin so it can be unit tested.
 */
class LaunchGuard(private val windowMillis: Long = DEFAULT_WINDOW_MILLIS) {

    private val launchedAt = mutableMapOf<String, Long>()

    fun onLaunched(packageName: String, nowMillis: Long) {
        launchedAt[packageName] = nowMillis
    }

    fun isExempt(packageName: String, nowMillis: Long): Boolean {
        val at = launchedAt[packageName] ?: return false
        if (nowMillis - at < windowMillis) return true
        launchedAt.remove(packageName)
        return false
    }

    companion object {
        const val DEFAULT_WINDOW_MILLIS = 5_000L
    }
}

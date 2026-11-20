package com.example.bourget.detection

/**
 * Decides whether a foreground-app change should trigger an intervention.
 *
 * Triggers only when the foreground moves *into* a target package from a different package,
 * and not again for the same package until [cooldownMillis] has elapsed.
 * Pure Kotlin (no Android types) so it can be unit tested.
 */
class AppLaunchDetector(
    val targetPackages: Set<String>,
    private val cooldownMillis: Long = DEFAULT_COOLDOWN_MILLIS,
) {
    private var foregroundPackage: String? = null
    private val lastTriggeredAt = mutableMapOf<String, Long>()

    /** Returns true if an intervention should run for [packageName]. */
    fun onForegroundChanged(packageName: String, nowMillis: Long): Boolean {
        val previous = foregroundPackage
        foregroundPackage = packageName
        if (packageName == previous || packageName !in targetPackages) return false

        val last = lastTriggeredAt[packageName]
        if (last != null && nowMillis - last < cooldownMillis) return false

        lastTriggeredAt[packageName] = nowMillis
        return true
    }

    companion object {
        const val DEFAULT_COOLDOWN_MILLIS = 30_000L
    }
}

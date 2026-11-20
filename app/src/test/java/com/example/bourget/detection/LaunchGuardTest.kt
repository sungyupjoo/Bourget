package com.example.bourget.detection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchGuardTest {

    private val yt = "com.google.android.youtube"
    private val ig = "com.instagram.android"
    private val launcher = "com.android.launcher"

    @Test
    fun launchedAppIsExemptOnlyInsideTheWindow() {
        val guard = LaunchGuard(windowMillis = 5_000)
        guard.onLaunched(ig, 1_000)
        assertTrue(guard.isExempt(ig, 5_999))
        assertFalse(guard.isExempt(ig, 6_000))
        assertFalse(guard.isExempt(yt, 1_000))
    }

    @Test
    fun appLaunchedByBourgetDoesNotTriggerItsOwnRule() {
        // YouTube's rule launches Instagram, and Instagram has a rule that launches YouTube.
        val detector = AppLaunchDetector(setOf(yt, ig), cooldownMillis = 30_000)
        val guard = LaunchGuard(windowMillis = 5_000)
        assertFalse(detector.onForegroundChanged(launcher, 0))
        assertTrue(detector.onForegroundChanged(yt, 1_000, guard.isExempt(yt, 1_000)))
        guard.onLaunched(ig, 1_000)
        assertFalse(detector.onForegroundChanged(ig, 1_500, guard.isExempt(ig, 1_500)))
        // Back to YouTube within its cooldown: no second intervention either.
        assertFalse(detector.onForegroundChanged(yt, 3_000, guard.isExempt(yt, 3_000)))
    }

    @Test
    fun exemptionDoesNotUseUpTheCooldown() {
        val detector = AppLaunchDetector(setOf(ig), cooldownMillis = 30_000)
        assertFalse(detector.onForegroundChanged(ig, 0, exempt = true))
        assertFalse(detector.onForegroundChanged(launcher, 1_000))
        // Opened by the user later: the rule applies, it was never "used" by the exempt visit.
        assertTrue(detector.onForegroundChanged(ig, 10_000))
    }
}

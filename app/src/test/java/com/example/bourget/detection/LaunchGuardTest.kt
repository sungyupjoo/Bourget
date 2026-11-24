package com.example.bourget.detection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchGuardTest {

    private val yt = "com.google.android.youtube"
    private val ig = "com.instagram.android"
    private val launcher = "com.android.launcher"
    private val other = "com.example.other"
    private val chrome = "com.android.chrome"

    @Test
    fun launchedAppIsExemptOnlyInsideTheWindow() {
        val guard = LaunchGuard(windowMillis = 5_000)
        guard.onLaunched(ig, yt, 1_000)
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
        guard.onLaunched(ig, yt, 1_000)
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

    /** Mirrors the service: a switch to [pkg] exempts it when it is a return from the launched app. */
    private fun AppLaunchDetector.switchTo(guard: LaunchGuard, pkg: String, now: Long): Boolean {
        val returned = guard.onSwitched(pkg, neutral = pkg == launcher)
        return onForegroundChanged(pkg, now, returned || guard.isExempt(pkg, now))
    }

    @Test
    fun comingBackFromTheLaunchedAppDoesNotLaunchItAgain() {
        val detector = AppLaunchDetector(setOf(yt), cooldownMillis = 30_000)
        val guard = LaunchGuard(windowMillis = 5_000)
        assertTrue(detector.switchTo(guard, yt, 0))
        guard.onLaunched(ig, yt, 0)
        assertFalse(detector.switchTo(guard, ig, 500))
        // Straight back, after the cooldown.
        assertFalse(detector.switchTo(guard, yt, 60_000))
    }

    @Test
    fun comingBackThroughHomeOrRecentsCountsAsAReturn() {
        val detector = AppLaunchDetector(setOf(yt), cooldownMillis = 30_000)
        val guard = LaunchGuard(windowMillis = 5_000)
        assertTrue(detector.switchTo(guard, yt, 0))
        guard.onLaunched(ig, yt, 0)
        assertFalse(detector.switchTo(guard, ig, 500))
        assertFalse(detector.switchTo(guard, launcher, 50_000))
        assertFalse(detector.switchTo(guard, ig, 51_000))
        assertFalse(detector.switchTo(guard, launcher, 52_000))
        assertFalse(detector.switchTo(guard, yt, 60_000))
    }

    @Test
    fun usingAnotherAppInBetweenEndsTheExemption() {
        val detector = AppLaunchDetector(setOf(yt), cooldownMillis = 30_000)
        val guard = LaunchGuard(windowMillis = 5_000)
        assertTrue(detector.switchTo(guard, yt, 0))
        guard.onLaunched(ig, yt, 0)
        assertFalse(detector.switchTo(guard, ig, 500))
        assertFalse(detector.switchTo(guard, other, 40_000))
        assertTrue(detector.switchTo(guard, yt, 60_000))
    }

    @Test
    fun noReturnWithoutReachingTheLaunchedApp() {
        // The launch failed: the user never got to Instagram.
        val detector = AppLaunchDetector(setOf(yt), cooldownMillis = 30_000)
        val guard = LaunchGuard(windowMillis = 5_000)
        assertTrue(detector.switchTo(guard, yt, 0))
        guard.onLaunched(ig, yt, 0)
        assertFalse(detector.switchTo(guard, launcher, 1_000))
        assertTrue(detector.switchTo(guard, yt, 60_000))
    }

    @Test
    fun theReturnIsExemptOnlyOnce() {
        val detector = AppLaunchDetector(setOf(yt), cooldownMillis = 30_000)
        val guard = LaunchGuard(windowMillis = 5_000)
        assertTrue(detector.switchTo(guard, yt, 0))
        guard.onLaunched(ig, yt, 0)
        assertFalse(detector.switchTo(guard, ig, 500))
        assertFalse(detector.switchTo(guard, yt, 10_000))
        // The exempt return did not use up the cooldown either: opened again later, the rule applies.
        assertFalse(detector.switchTo(guard, launcher, 20_000))
        assertTrue(detector.switchTo(guard, yt, 40_000))
    }

    @Test
    fun comingBackToTheBrowserFromAppLaunchedByAWebsiteRule() {
        val guard = LaunchGuard(windowMillis = 5_000)
        guard.onLaunched(ig, chrome, 0)
        assertFalse(guard.onSwitched(ig, neutral = false))
        assertFalse(guard.onSwitched(launcher, neutral = true))
        assertTrue(guard.onSwitched(chrome, neutral = false))
        // Only once.
        assertFalse(guard.onSwitched(launcher, neutral = true))
        assertFalse(guard.onSwitched(chrome, neutral = false))
    }
}

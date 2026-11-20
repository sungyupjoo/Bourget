package com.example.bourget.detection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLaunchDetectorTest {

    private val yt = "com.google.android.youtube"
    private val launcher = "com.android.launcher"
    private val other = "com.example.other"

    private fun detector() = AppLaunchDetector(setOf(yt), cooldownMillis = 30_000)

    @Test
    fun triggersWhenTargetComesToForeground() {
        val d = detector()
        assertFalse(d.onForegroundChanged(launcher, 0))
        assertTrue(d.onForegroundChanged(yt, 1_000))
    }

    @Test
    fun doesNotTriggerForNonTarget() {
        assertFalse(detector().onForegroundChanged(other, 0))
    }

    @Test
    fun repeatedEventsInsideTargetDoNotRetrigger() {
        val d = detector()
        assertTrue(d.onForegroundChanged(yt, 0))
        // Activity changes inside YouTube, even after the cooldown.
        assertFalse(d.onForegroundChanged(yt, 500))
        assertFalse(d.onForegroundChanged(yt, 60_000))
    }

    @Test
    fun reEnteringWithinCooldownDoesNotRetrigger() {
        val d = detector()
        assertTrue(d.onForegroundChanged(yt, 0))
        d.onForegroundChanged(launcher, 5_000)
        assertFalse(d.onForegroundChanged(yt, 29_999))
    }

    @Test
    fun reEnteringAfterCooldownTriggersAgain() {
        val d = detector()
        assertTrue(d.onForegroundChanged(yt, 0))
        d.onForegroundChanged(launcher, 5_000)
        assertTrue(d.onForegroundChanged(yt, 30_000))
    }
}

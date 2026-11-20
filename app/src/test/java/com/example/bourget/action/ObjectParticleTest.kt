package com.example.bourget.action

import org.junit.Assert.assertEquals
import org.junit.Test

class ObjectParticleTest {

    @Test
    fun finalConsonantTakesEul() {
        assertEquals("을", objectParticle("인스타그램"))
        assertEquals("을", objectParticle("카카오톡"))
    }

    @Test
    fun noFinalConsonantTakesReul() {
        assertEquals("를", objectParticle("유튜브"))
        assertEquals("를", objectParticle("크롬 브라우저"))
    }

    @Test
    fun nonHangulEndingFallsBack() {
        assertEquals("을(를)", objectParticle("YouTube"))
        assertEquals("을(를)", objectParticle(""))
    }
}

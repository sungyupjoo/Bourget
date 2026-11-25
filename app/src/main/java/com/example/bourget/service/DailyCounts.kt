package com.example.bourget.service

import java.util.Calendar

/**
 * Today's confirm popups (or launches of another app) and "continue" taps per rule, shown on the
 * popup or in the short notice after a launch. Kept in memory only: never written to disk, cleared when
 * the day changes and gone when the service stops.
 */
class DailyCounts {

    private var day = today()
    private val counts = HashMap<String, IntArray>()

    /** Counts a popup or launch for [key]; returns (today's popups including this one, continues before it). */
    fun onShown(key: String): Pair<Int, Int> {
        val count = countFor(key)
        count[SHOWN]++
        return count[SHOWN] to count[CONTINUED]
    }

    fun onContinued(key: String) {
        countFor(key)[CONTINUED]++
    }

    private fun countFor(key: String): IntArray {
        val now = today()
        if (now != day) {
            day = now
            counts.clear()
        }
        return counts.getOrPut(key) { IntArray(2) }
    }

    private fun today(): Int = Calendar.getInstance().let { it[Calendar.YEAR] * 1000 + it[Calendar.DAY_OF_YEAR] }

    private companion object {
        const val SHOWN = 0
        const val CONTINUED = 1
    }
}

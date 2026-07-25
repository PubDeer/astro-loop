package com.astroloop.game.render

import org.junit.Assert.*
import org.junit.Test

class HUDTimerFormatTest {

    private fun formatTimer(totalSeconds: Int): String {
        val mins = totalSeconds / 60
        val secs = totalSeconds % 60
        return if (mins >= 1) {
            "$mins:${secs.toString().padStart(2, '0')}"
        } else {
            "$secs"
        }
    }

    @Test
    fun `formats seconds only when under one minute`() {
        assertEquals("45", formatTimer(45))
        assertEquals("0", formatTimer(0))
        assertEquals("59", formatTimer(59))
    }

    @Test
    fun `formats minutes and zero-padded seconds`() {
        assertEquals("1:00", formatTimer(60))
        assertEquals("1:05", formatTimer(65))
        assertEquals("10:30", formatTimer(630))
    }

    @Test
    fun `formats two-digit minutes`() {
        assertEquals("12:34", formatTimer(754))
    }
}

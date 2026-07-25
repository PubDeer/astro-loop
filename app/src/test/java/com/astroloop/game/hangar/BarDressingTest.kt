// app/src/test/java/com/astroloop/game/hangar/BarDressingTest.kt
package com.astroloop.game.hangar

import com.astroloop.game.core.StoryStage
import org.junit.Assert.*
import org.junit.Test

class BarDressingTest {

    @Test
    fun `normal stage is warm amber with steady sign and string lights`() {
        val d = BarDressing.forStage(StoryStage.NORMAL)
        assertEquals(SignMode.STEADY, d.signMode)
        assertTrue(d.stringLights)
        assertFalse(d.fairyLights)
        assertTrue(d.seatedCrew)
        assertFalse(d.confetti)
        assertEquals(7, d.lampColors.size)
        // first warm bulb
        assertEquals(0xFFFFB366.toInt(), d.lampColors[0])
    }

    @Test
    fun `corruption stage is all red lamps with a blinking sign and no props`() {
        val d = BarDressing.forStage(StoryStage.CORRUPTION)
        assertEquals(SignMode.BLINKING, d.signMode)
        assertFalse(d.stringLights)
        assertFalse(d.fairyLights)
        assertFalse(d.seatedCrew)
        assertFalse(d.confetti)
        assertEquals(7, d.lampColors.size)
        assertTrue("all lamps corruption red", d.lampColors.all { it == 0xFFAA2222.toInt() })
    }

    @Test
    fun `astro loop stage is festival with rainbow lamps and all props`() {
        val d = BarDressing.forStage(StoryStage.ASTRO_LOOP)
        assertEquals(SignMode.STEADY, d.signMode)
        assertFalse(d.stringLights)
        assertTrue(d.fairyLights)
        assertTrue(d.seatedCrew)
        assertTrue(d.confetti)
        // shipped rainbow palette
        assertEquals(0xFFFF0066.toInt(), d.lampColors[0])
    }
}

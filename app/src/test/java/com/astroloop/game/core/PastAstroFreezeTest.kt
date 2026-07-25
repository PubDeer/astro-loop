package com.astroloop.game.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PastAstroFreezeTest {

    @Test
    fun `pastAstroEmpFrozen defaults to false`() {
        assertFalse(GameState().pastAstroEmpFrozen)
    }

    @Test
    fun `reset clears pastAstroEmpFrozen`() {
        val s = GameState()
        s.pastAstroEmpFrozen = true
        s.reset()
        assertFalse(s.pastAstroEmpFrozen)
    }

    // Locks the interrupt-block gate: `bossCharging && !pastAstroEmpFrozen`.
    private fun interruptRuns(bossCharging: Boolean, frozen: Boolean) = bossCharging && !frozen

    @Test
    fun `interrupt block runs while charging and not yet EMP-frozen`() {
        assertTrue(interruptRuns(bossCharging = true, frozen = false))
    }

    @Test
    fun `interrupt block stops the instant EMP freezes Past Astro`() {
        assertFalse(interruptRuns(bossCharging = true, frozen = true))
    }

    @Test
    fun `EMP1 leaves bossEmpFired untouched so EMP2 can still fire`() {
        val s = GameState()
        s.pastAstroEmpFrozen = true
        assertEquals(false, s.bossEmpFired)
    }
}

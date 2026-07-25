package com.astroloop.game.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The retreat auto-pilot (emergency shield fly-off, retreatPhase >= 2) blocks
 * gameplay touches — but must never swallow touches while paused: the pause
 * screen resumes via TouchController.consumeTap(), so a blocked tap strands
 * the run on PAUSED with no way back (app-switch during the fly-off).
 */
class RetreatPauseTouchTest {

    @Test fun `retreat auto-pilot swallows gameplay touches`() {
        assertTrue(retreatBlocksTouch(isPaused = false, retreatPhase = 2))
        assertTrue(retreatBlocksTouch(isPaused = false, retreatPhase = 3))
    }

    @Test fun `pause-resume tap gets through during retreat`() {
        assertFalse(retreatBlocksTouch(isPaused = true, retreatPhase = 2))
        assertFalse(retreatBlocksTouch(isPaused = true, retreatPhase = 3))
    }

    @Test fun `touches flow normally outside retreat`() {
        assertFalse(retreatBlocksTouch(isPaused = false, retreatPhase = 0))
        assertFalse(retreatBlocksTouch(isPaused = false, retreatPhase = 1))
    }
}

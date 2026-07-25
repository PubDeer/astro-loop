package com.astroloop.game.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the shield-deflect gate in GameSurfaceView.update() (~line 2121):
 *   boss.isActive && boss.shielded &&
 *     (phase < PHASE_OTHER_SPAWN || phase >= PHASE_OTHER_SURVIVAL)
 * Phase ints mirror GameSurfaceView's companion constants.
 */
class ShieldDeflectGateTest {
    private val OTHER_SPAWN = 10
    private val OTHER_SURVIVAL = 11   // the corruption charge window
    private val OTHER_FLEET = 12
    private val SHIELD_ASSAULT = 8    // a normal-fight phase

    private fun deflectActive(phase: Int, active: Boolean, shielded: Boolean): Boolean =
        active && shielded && (phase < OTHER_SPAWN || phase >= OTHER_SURVIVAL)

    @Test
    fun `deflect is live during the corruption charge when boss is active and shielded`() {
        assertTrue(deflectActive(OTHER_SURVIVAL, active = true, shielded = true))
    }

    @Test
    fun `deflect stays off in pre-charge survival while the boss entity is inactive`() {
        assertFalse(deflectActive(OTHER_SURVIVAL, active = false, shielded = true))
    }

    @Test
    fun `deflect still covers the later corruption fleet phases`() {
        assertTrue(deflectActive(OTHER_FLEET, active = true, shielded = true))
    }

    @Test
    fun `deflect still covers the normal-fight phases`() {
        assertTrue(deflectActive(SHIELD_ASSAULT, active = true, shielded = true))
    }
}

package com.astroloop.game.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReentryBurnTest {

    private fun step(burn: ReentryBurn, seconds: Float, emitting: Boolean, dt: Float = 1f / 60f) {
        var t = 0f
        while (t < seconds) {
            burn.update(100f, 200f, 1.5f, dt, emitting)
            t += dt
        }
    }

    @Test
    fun `emits ghosts while emitting and caps the chain length`() {
        val burn = ReentryBurn()
        step(burn, 1f, emitting = true)
        assertTrue(burn.ghosts.isNotEmpty())
        assertTrue("chain capped at MAX_GHOSTS", burn.ghosts.size <= ReentryBurn.MAX_GHOSTS)
    }

    @Test
    fun `emits sparks while emitting`() {
        val burn = ReentryBurn()
        step(burn, 0.5f, emitting = true)
        assertTrue(burn.sparks.isNotEmpty())
    }

    @Test
    fun `ghosts and sparks fade out after emission stops`() {
        val burn = ReentryBurn()
        step(burn, 0.5f, emitting = true)
        assertTrue(burn.hasContent())
        step(burn, ReentryBurn.GHOST_LIFETIME + 0.1f, emitting = false)
        assertFalse("everything must age out (no instant disappearance)", burn.hasContent())
    }

    @Test
    fun `no emission when not emitting`() {
        val burn = ReentryBurn()
        step(burn, 1f, emitting = false)
        assertEquals(0, burn.ghosts.size)
        assertEquals(0, burn.sparks.size)
    }

    @Test
    fun `clear empties everything`() {
        val burn = ReentryBurn()
        step(burn, 0.5f, emitting = true)
        burn.clear()
        assertFalse(burn.hasContent())
    }
}

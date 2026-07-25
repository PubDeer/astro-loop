package com.astroloop.game.core

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Astro Loop only: the active pilot's earned bandana shows on their own
 * radio chatter portrait. radioBandanaPilotId() is the pure gate the HUD
 * passes to IconCache.getPortraitByCallsign().
 */
class GameStateRadioBandanaTest {

    private lateinit var state: GameState

    @Before
    fun setup() {
        state = GameState()
        state.reset()
        state.activePilotId = "pilot_dash"
    }

    @Test
    fun `null outside astro loop even with bandana`() {
        state.astroLoopMode = false
        state.activePilotHasBandana = true
        assertNull(state.radioBandanaPilotId())
    }

    @Test
    fun `null in astro loop without bandana`() {
        state.astroLoopMode = true
        state.activePilotHasBandana = false
        assertNull(state.radioBandanaPilotId())
    }

    @Test
    fun `active pilot id in astro loop with bandana`() {
        state.astroLoopMode = true
        state.activePilotHasBandana = true
        assertEquals("pilot_dash", state.radioBandanaPilotId())
    }
}

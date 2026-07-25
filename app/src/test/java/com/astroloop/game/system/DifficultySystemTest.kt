package com.astroloop.game.system

import com.astroloop.game.core.GameState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DifficultySystemTest {

    private lateinit var system: DifficultySystem
    private lateinit var state: GameState

    @Before
    fun setup() {
        system = DifficultySystem()
        state = GameState()
        state.reset()
    }

    @Test
    fun `normal mode caps at 4_0 at peak`() {
        state.survivalTime = 510.6f  // ~8.51 minutes — parabola peak
        system.update(0f, state)
        assertEquals(4.0f, state.difficultyMultiplier, 0.01f)
    }

    @Test
    fun `normal mode has not yet capped at 7 minutes`() {
        state.survivalTime = 420f  // 7 minutes
        system.update(0f, state)
        // 1 + 0.706*7 - 0.0415*49 = 3.908, below the 4.0 cap
        assertEquals(3.908f, state.difficultyMultiplier, 0.02f)
        assertTrue(state.difficultyMultiplier < 4.0f)
    }

    @Test
    fun `astro loop mode exceeds 4_0 after 8 minutes`() {
        state.astroLoopMode = true
        state.survivalTime = 660f  // 11 minutes
        system.update(0f, state)
        assertTrue("Should exceed 4.0 in astro loop", state.difficultyMultiplier > 4.0f)
    }

    @Test
    fun `astro loop mode linear ramp is continuous at peak`() {
        state.astroLoopMode = true
        state.survivalTime = 480f  // 8.0 minutes = new peak
        system.update(0f, state)
        assertEquals(4.0f, state.difficultyMultiplier, 0.02f)
    }

    @Test
    fun `astro loop mode grows linearly beyond peak`() {
        state.astroLoopMode = true
        state.survivalTime = 480f + 60f  // 1 minute past the 8-min peak
        system.update(0f, state)
        // 4.0 + 0.4 * 1 = 4.4
        assertEquals(4.4f, state.difficultyMultiplier, 0.05f)
    }

    @Test
    fun `astro loop mode before peak uses astro quad`() {
        state.astroLoopMode = true
        state.survivalTime = 120f  // 2 minutes
        system.update(0f, state)
        // 1 + 0.75*2 - 0.046875*4 = 2.3125
        assertEquals(2.3125f, state.difficultyMultiplier, 0.02f)
    }

    @Test
    fun `astro loop ramps at least as fast as normal in early game`() {
        val stateNormal = GameState().also { it.reset(); it.survivalTime = 180f }
        val stateLoop   = GameState().also { it.reset(); it.survivalTime = 180f; it.astroLoopMode = true }
        system.update(0f, stateNormal)
        system.update(0f, stateLoop)
        // Tuning pass: astro quad (0.75) is steeper than normal (0.706)
        assertTrue(
            "Astro Loop should ramp at least as fast as normal at 3 min",
            stateLoop.difficultyMultiplier >= stateNormal.difficultyMultiplier
        )
    }
}

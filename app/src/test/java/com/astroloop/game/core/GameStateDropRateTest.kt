package com.astroloop.game.core

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for asteroid upgrade drop rate scaling.
 * Early game has higher drop rates that decrease with each upgrade collected.
 * Salvage multiplier (0.5x base, +10% per level) applies on top.
 */
class GameStateDropRateTest {

    private lateinit var state: GameState

    @Before
    fun setup() {
        state = GameState()
        state.reset()
    }

    // ─── Initial Drop Rate (with 0.5x salvage base) ─────────────

    @Test
    fun `initial drop chance is 5 percent with no salvage`() {
        // Base 10% * 0.5 salvage multiplier = 5%
        assertEquals(0.05f, state.getAsteroidDropChance(), 0.001f)
    }

    @Test
    fun `initially in early game drop rate`() {
        assertTrue(state.isEarlyGameDropRate())
    }

    // ─── Drop Rate Decreases ───────────────────────────────────

    @Test
    fun `drop chance decreases with asteroid upgrades`() {
        state.asteroidUpgradesCollected = 1
        // (10% - 1%) * 0.5 = 4.5%
        assertEquals(0.045f, state.getAsteroidDropChance(), 0.001f)

        state.asteroidUpgradesCollected = 5
        // (10% - 5%) * 0.5 = 2.5%
        assertEquals(0.025f, state.getAsteroidDropChance(), 0.001f)
    }

    // ─── Baseline Floor ────────────────────────────────────────

    @Test
    fun `drop chance floors at baseline times salvage`() {
        state.asteroidUpgradesCollected = 100
        // 2% baseline * 0.5 = 1%
        assertEquals(0.01f, state.getAsteroidDropChance(), 0.001f)
    }

    @Test
    fun `exits early game when reaching baseline`() {
        state.asteroidUpgradesCollected = 5
        assertTrue(state.isEarlyGameDropRate())

        state.asteroidUpgradesCollected = 20
        assertFalse(state.isEarlyGameDropRate())
    }

    // ─── Salvage Permanent ───────────────────────────────────

    @Test
    fun `salvage level 5 restores original drop rate`() {
        state.permanentSalvageLevel = 5
        // 10% * 1.0 = 10%
        assertEquals(0.10f, state.getAsteroidDropChance(), 0.001f)
    }

    @Test
    fun `salvage level 3 partially restores drop rate`() {
        state.permanentSalvageLevel = 3
        // 10% * 0.8 = 8%
        assertEquals(0.08f, state.getAsteroidDropChance(), 0.001f)
    }

    // ─── Reset Behavior ────────────────────────────────────────

    @Test
    fun `reset restores initial drop rate`() {
        state.asteroidUpgradesCollected = 10
        state.reset()
        assertEquals(0, state.asteroidUpgradesCollected)
        assertEquals(0.05f, state.getAsteroidDropChance(), 0.001f)
        assertTrue(state.isEarlyGameDropRate())
    }

    // ─── Astro Loop Mode (boosted rates, no enemy drops) ───────

    @Test
    fun `astro loop initial drop chance is 5 percent with no salvage`() {
        // Base 10% * 0.5 salvage multiplier = 5%
        state.astroLoopMode = true
        assertEquals(0.05f, state.getAsteroidDropChance(), 0.001f)
    }

    @Test
    fun `astro loop is initially in early game drop rate`() {
        state.astroLoopMode = true
        assertTrue(state.isEarlyGameDropRate())
    }

    @Test
    fun `astro loop drop chance decreases with asteroid upgrades`() {
        state.astroLoopMode = true
        state.asteroidUpgradesCollected = 1
        // (10% - 1%) * 0.5 = 4.5%
        assertEquals(0.045f, state.getAsteroidDropChance(), 0.001f)

        state.asteroidUpgradesCollected = 3
        // (10% - 3%) * 0.5 = 3.5%
        assertEquals(0.035f, state.getAsteroidDropChance(), 0.001f)
    }

    @Test
    fun `astro loop drop chance floors at astro loop baseline times salvage`() {
        state.astroLoopMode = true
        state.asteroidUpgradesCollected = 100
        // 5% baseline * 0.5 = 2.5%
        assertEquals(0.025f, state.getAsteroidDropChance(), 0.001f)
    }

    @Test
    fun `astro loop reaches its drop-rate floor sooner than normal due to higher baseline`() {
        // Astro Loop floors at the 5% baseline: early game ends once (10% - 1%*n) reaches 5%.
        state.astroLoopMode = true
        state.asteroidUpgradesCollected = 3
        assertTrue("astro still early at 3 upgrades (7% > 5%)", state.isEarlyGameDropRate())
        state.asteroidUpgradesCollected = 7
        assertFalse("astro at its 5% floor by 7 upgrades", state.isEarlyGameDropRate())

        // Normal mode floors at the lower 2% baseline, so it stays in early game longer.
        state.astroLoopMode = false
        state.asteroidUpgradesCollected = 7
        assertTrue("normal still early at 7 upgrades (3% > 2%)", state.isEarlyGameDropRate())
        state.asteroidUpgradesCollected = 10
        assertFalse("normal at its 2% floor by 10 upgrades", state.isEarlyGameDropRate())
    }

    @Test
    fun `astro loop salvage level 5 restores full drop rate`() {
        state.astroLoopMode = true
        state.permanentSalvageLevel = 5
        // 10% * 1.0 = 10%
        assertEquals(0.10f, state.getAsteroidDropChance(), 0.001f)
    }
}

package com.astroloop.game.core

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for weapon management in GameState.
 * Complements GameStateSlotTest with more detailed weapon behavior tests.
 */
class GameStateWeaponTest {

    private lateinit var state: GameState

    @Before
    fun setup() {
        state = GameState()
        state.reset()
    }

    // ─── Weapon Adding Behavior ──────────────────────────────────────

    @Test
    fun `adding new weapon sets level to 1`() {
        state.addWeapon("pulse_cannon")
        assertEquals(1, state.getWeaponLevel("pulse_cannon"))
    }

    @Test
    fun `adding same weapon increments level`() {
        state.addWeapon("pulse_cannon")
        state.addWeapon("pulse_cannon")
        assertEquals(2, state.getWeaponLevel("pulse_cannon"))
    }

    @Test
    fun `weapon levels progress correctly to max`() {
        for (expectedLevel in 1..5) {
            state.addWeapon("railgun")
            assertEquals(expectedLevel, state.getWeaponLevel("railgun"))
        }
    }

    @Test
    fun `getWeaponLevel returns 0 for unowned weapons`() {
        assertEquals(0, state.getWeaponLevel("nonexistent_weapon"))
        assertEquals(0, state.getWeaponLevel("pulse_cannon"))
    }

    @Test
    fun `addWeapon returns true when successful`() {
        assertTrue(state.addWeapon("pulse_cannon"))
        assertTrue(state.addWeapon("pulse_cannon"))  // Level 2
    }

    @Test
    fun `addWeapon returns false when at max level`() {
        repeat(5) { state.addWeapon("pulse_cannon") }
        assertFalse(state.addWeapon("pulse_cannon"))
    }

    @Test
    fun `different weapons track independently`() {
        repeat(3) { state.addWeapon("pulse_cannon") }
        repeat(2) { state.addWeapon("railgun") }
        state.addWeapon("scatter_shot")

        assertEquals(3, state.getWeaponLevel("pulse_cannon"))
        assertEquals(2, state.getWeaponLevel("railgun"))
        assertEquals(1, state.getWeaponLevel("scatter_shot"))
    }

    // ─── Weapon Slot Management ──────────────────────────────────────

    @Test
    fun `weapon count increases with new weapons only`() {
        assertEquals(0, state.getWeaponCount())

        state.addWeapon("pulse_cannon")
        assertEquals(1, state.getWeaponCount())

        state.addWeapon("pulse_cannon")  // Level up
        assertEquals(1, state.getWeaponCount())

        state.addWeapon("railgun")
        assertEquals(2, state.getWeaponCount())
    }

    @Test
    fun `canAddNewWeapon checks slot availability`() {
        assertTrue(state.canAddNewWeapon())

        // Fill slots
        state.addWeapon("pulse_cannon")
        state.addWeapon("railgun")
        state.addWeapon("scatter_shot")
        assertTrue(state.canAddNewWeapon())

        state.addWeapon("homing_missiles")
        assertFalse(state.canAddNewWeapon())
    }

    @Test
    fun `canAddNewWeapon does not consider level ups`() {
        state.addWeapon("pulse_cannon")
        state.addWeapon("railgun")
        state.addWeapon("scatter_shot")
        state.addWeapon("homing_missiles")

        assertFalse(state.canAddNewWeapon())

        // Level up existing weapon
        state.addWeapon("pulse_cannon")

        // Still can't add new weapon
        assertFalse(state.canAddNewWeapon())
    }

    // ─── Evolution Tracking ──────────────────────────────────────────

    @Test
    fun `hasEvolution returns false for unevolved weapons`() {
        assertFalse(state.hasEvolution("storm_cannon"))
    }

    @Test
    fun `addEvolution marks weapon as evolved`() {
        state.addEvolution("storm_cannon")
        assertTrue(state.hasEvolution("storm_cannon"))
    }

    @Test
    fun `evolutions track independently`() {
        state.addEvolution("storm_cannon")
        state.addEvolution("oblivion_beam")

        assertTrue(state.hasEvolution("storm_cannon"))
        assertTrue(state.hasEvolution("oblivion_beam"))
        assertFalse(state.hasEvolution("warp_saw"))
    }

    @Test
    fun `adding same evolution twice is idempotent`() {
        state.addEvolution("storm_cannon")
        state.addEvolution("storm_cannon")

        assertTrue(state.hasEvolution("storm_cannon"))
    }

    // ─── Reset Behavior ──────────────────────────────────────────────

    @Test
    fun `reset clears weapon levels`() {
        state.addWeapon("pulse_cannon")
        state.addWeapon("railgun")

        state.reset()

        assertEquals(0, state.getWeaponLevel("pulse_cannon"))
        assertEquals(0, state.getWeaponLevel("railgun"))
        assertEquals(0, state.getWeaponCount())
    }

    @Test
    fun `reset clears evolutions`() {
        state.addEvolution("storm_cannon")
        state.addEvolution("oblivion_beam")

        state.reset()

        assertFalse(state.hasEvolution("storm_cannon"))
        assertFalse(state.hasEvolution("oblivion_beam"))
    }

    @Test
    fun `reset restores ability to add weapons`() {
        state.addWeapon("pulse_cannon")
        state.addWeapon("railgun")
        state.addWeapon("scatter_shot")
        state.addWeapon("homing_missiles")
        assertFalse(state.canAddNewWeapon())

        state.reset()

        assertTrue(state.canAddNewWeapon())
    }

    // ─── Upgrade Count Integration ───────────────────────────────────

    @Test
    fun `weapon additions increment total upgrade count`() {
        assertEquals(0, state.totalUpgradesCollected)

        state.addWeapon("pulse_cannon")
        assertEquals(1, state.totalUpgradesCollected)

        state.addWeapon("pulse_cannon")
        assertEquals(2, state.totalUpgradesCollected)

        state.addWeapon("railgun")
        assertEquals(3, state.totalUpgradesCollected)
    }

    @Test
    fun `failed weapon additions do not increment count`() {
        repeat(5) { state.addWeapon("pulse_cannon") }
        assertEquals(5, state.totalUpgradesCollected)

        state.addWeapon("pulse_cannon")  // Should fail
        assertEquals(5, state.totalUpgradesCollected)
    }
}

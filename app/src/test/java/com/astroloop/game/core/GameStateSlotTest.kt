package com.astroloop.game.core

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for weapon/passive slot management and upgrade limits.
 */
class GameStateSlotTest {

    private lateinit var state: GameState

    @Before
    fun setup() {
        state = GameState()
        state.reset()
    }

    // ─── Weapon Slots ──────────────────────────────────────────

    @Test
    fun `default max weapon slots is 4`() {
        assertEquals(4, state.getMaxWeaponSlots())
    }

    @Test
    fun `can add weapons up to slot limit`() {
        assertTrue(state.canAddNewWeapon())

        state.addWeapon("pulse_cannon")
        state.addWeapon("scatter_shot")
        state.addWeapon("railgun")
        assertTrue(state.canAddNewWeapon())

        state.addWeapon("homing_missiles")
        assertFalse(state.canAddNewWeapon())
        assertEquals(4, state.getWeaponCount())
    }

    @Test
    fun `weapon levels cap at 5`() {
        repeat(10) { state.addWeapon("pulse_cannon") }

        assertEquals(5, state.getWeaponLevel("pulse_cannon"))
    }

    @Test
    fun `addWeapon returns false when at max level`() {
        repeat(5) {
            assertTrue(state.addWeapon("pulse_cannon"))
        }
        assertFalse(state.addWeapon("pulse_cannon"))
    }

    @Test
    fun `leveling existing weapon does not use new slot`() {
        state.addWeapon("pulse_cannon")
        assertEquals(1, state.getWeaponCount())

        state.addWeapon("pulse_cannon")
        assertEquals(1, state.getWeaponCount())
        assertEquals(2, state.getWeaponLevel("pulse_cannon"))
    }

    // ─── Passive Slots ─────────────────────────────────────────

    @Test
    fun `default max passive slots is 4`() {
        assertEquals(4, state.getMaxPassiveSlots())
    }

    @Test
    fun `can add passives up to slot limit`() {
        assertTrue(state.canAddNewPassive())

        state.addPassive("nano_repair")
        state.addPassive("magnet_field")
        state.addPassive("phoenix_core")
        assertTrue(state.canAddNewPassive())

        state.addPassive("lucky_star")
        assertFalse(state.canAddNewPassive())
        assertEquals(4, state.getPassiveCount())
    }

    @Test
    fun `stacking existing passive does not use new slot`() {
        state.addPassive("nano_repair")
        assertEquals(1, state.getPassiveCount())

        state.addPassive("nano_repair")
        assertEquals(1, state.getPassiveCount())
        assertEquals(2, state.getPassiveStacks("nano_repair"))
    }

    // ─── Extra Weapon Slot Passive ─────────────────────────────

    @Test
    fun `extra_weapon_slot increases weapon slots to 5`() {
        state.addPassive("extra_weapon_slot")
        assertEquals(5, state.getMaxWeaponSlots())
    }

    @Test
    fun `extra_weapon_slot decreases passive slots to 3`() {
        state.addPassive("extra_weapon_slot")
        assertEquals(3, state.getMaxPassiveSlots())
    }

    @Test
    fun `can add 5th weapon after extra_weapon_slot`() {
        state.addWeapon("pulse_cannon")
        state.addWeapon("scatter_shot")
        state.addWeapon("railgun")
        state.addWeapon("homing_missiles")
        assertFalse(state.canAddNewWeapon())

        state.addPassive("extra_weapon_slot")
        assertTrue(state.canAddNewWeapon())

        state.addWeapon("nova_blast")
        assertEquals(5, state.getWeaponCount())
        assertFalse(state.canAddNewWeapon())
    }

    // ─── extra_weapon_slot passive slot behaviour ──────────────────

    @Test
    fun `extra_weapon_slot is not counted in getPassiveCount`() {
        state.addPassive("extra_weapon_slot")
        // extra_weapon_slot should not consume a displayed passive slot
        assertEquals(0, state.getPassiveCount())
    }

    @Test
    fun `with extra_weapon_slot and 2 real passives canAddNewPassive is true`() {
        state.addPassive("extra_weapon_slot")
        state.addPassive("nano_repair")
        state.addPassive("magnet_field")
        // 3 max passive slots; extra_weapon_slot not counted; 2 real passives → 1 slot free
        assertTrue(state.canAddNewPassive())
    }

    @Test
    fun `with extra_weapon_slot and 3 real passives canAddNewPassive is false`() {
        state.addPassive("extra_weapon_slot")
        state.addPassive("nano_repair")
        state.addPassive("magnet_field")
        state.addPassive("vampiric_core")
        // 3 max passive slots, all 3 filled with real passives
        assertFalse(state.canAddNewPassive())
        assertEquals(3, state.getPassiveCount())
    }

    // ─── Fully Upgraded Check ──────────────────────────────────

    @Test
    fun `isFullyUpgraded returns false when empty`() {
        assertFalse(state.isFullyUpgraded())
    }

    @Test
    fun `isFullyUpgraded returns false when slots not filled`() {
        state.addWeapon("pulse_cannon")
        repeat(5) { state.addWeapon("pulse_cannon") }  // max level

        assertFalse(state.isFullyUpgraded())
    }

    @Test
    fun `isFullyUpgraded returns false when not all maxed`() {
        // Fill all slots but don't max them
        state.addWeapon("pulse_cannon")
        state.addWeapon("scatter_shot")
        state.addWeapon("railgun")
        state.addWeapon("homing_missiles")

        state.addPassive("nano_repair")
        state.addPassive("magnet_field")
        state.addPassive("phoenix_core")
        state.addPassive("lucky_star")

        assertFalse(state.isFullyUpgraded())
    }

    @Test
    fun `isFullyUpgraded returns true when all slots filled and maxed`() {
        // Fill and max all weapon slots
        val weapons = listOf("pulse_cannon", "scatter_shot", "railgun", "homing_missiles")
        weapons.forEach { weapon ->
            repeat(5) { state.addWeapon(weapon) }
        }

        // Fill and max all passive slots
        val passives = listOf("nano_repair", "magnet_field", "phoenix_core", "lucky_star")
        passives.forEach { passive ->
            repeat(5) { state.addPassive(passive) }
        }

        assertTrue(state.isFullyUpgraded())
    }

    @Test
    fun `isFullyUpgraded accounts for extra_weapon_slot`() {
        // Max out extra_weapon_slot first (5 stacks)
        repeat(5) { state.addPassive("extra_weapon_slot") }

        // Now need 5 weapons maxed
        val weapons = listOf("pulse_cannon", "scatter_shot", "railgun", "homing_missiles", "nova_blast")
        weapons.forEach { weapon ->
            repeat(5) { state.addWeapon(weapon) }
        }

        // extra_weapon_slot is excluded from getPassiveCount(), so all 3 real passive slots
        // (getMaxPassiveSlots() == 3 with extra_weapon_slot) must be filled and maxed
        val passives = listOf("nano_repair", "magnet_field", "vampiric_core")
        passives.forEach { passive ->
            repeat(5) { state.addPassive(passive) }
        }

        assertTrue(state.isFullyUpgraded())
    }

    // ─── Evolution Tracking ────────────────────────────────────

    @Test
    fun `can track evolved weapons`() {
        assertFalse(state.hasEvolution("storm_cannon"))

        state.addEvolution("storm_cannon")
        assertTrue(state.hasEvolution("storm_cannon"))
    }

    @Test
    fun `multiple evolutions tracked independently`() {
        state.addEvolution("storm_cannon")
        state.addEvolution("warp_saw")

        assertTrue(state.hasEvolution("storm_cannon"))
        assertTrue(state.hasEvolution("warp_saw"))
        assertFalse(state.hasEvolution("frost_ring"))
    }

    // ─── Upgrade Count Tracking ────────────────────────────────

    @Test
    fun `totalUpgradesCollected increments on weapon add`() {
        assertEquals(0, state.totalUpgradesCollected)

        state.addWeapon("pulse_cannon")
        assertEquals(1, state.totalUpgradesCollected)

        state.addWeapon("pulse_cannon")
        assertEquals(2, state.totalUpgradesCollected)
    }

    @Test
    fun `totalUpgradesCollected increments on passive add`() {
        assertEquals(0, state.totalUpgradesCollected)

        state.addPassive("nano_repair")
        assertEquals(1, state.totalUpgradesCollected)

        state.addPassive("nano_repair")
        assertEquals(2, state.totalUpgradesCollected)
    }

    @Test
    fun `totalUpgradesCollected does not increment when at max`() {
        repeat(5) { state.addWeapon("pulse_cannon") }
        assertEquals(5, state.totalUpgradesCollected)

        state.addWeapon("pulse_cannon")  // should fail, already maxed
        assertEquals(5, state.totalUpgradesCollected)
    }
}

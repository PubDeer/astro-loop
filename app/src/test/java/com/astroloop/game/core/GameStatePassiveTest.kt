package com.astroloop.game.core

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for passive effect calculations in GameState.
 * Each passive has specific scaling rules that must be verified.
 */
class GameStatePassiveTest {

    private lateinit var state: GameState

    @Before
    fun setup() {
        state = GameState()
        state.reset()
    }

    // ─── Nano Repair ───────────────────────────────────────────

    @Test
    fun `nano_repair adds 0_4 health regen per stack`() {
        assertEquals(0f, state.healthRegen, 0.001f)

        state.addPassive("nano_repair")
        assertEquals(0.4f, state.healthRegen, 0.001f)

        state.addPassive("nano_repair")
        assertEquals(0.8f, state.healthRegen, 0.001f)

        repeat(3) { state.addPassive("nano_repair") }
        assertEquals(2.0f, state.healthRegen, 0.001f)
    }

    // ─── Duplicator Core ───────────────────────────────────────

    @Test
    fun `duplicator_core gives instant-max 1 extra projectile`() {
        assertEquals(0, state.extraProjectiles)

        state.addPassive("duplicator_core")  // instant-max: 5 stacks, +1 projectile (binary effect)
        assertEquals(1, state.extraProjectiles)
    }

    @Test
    fun `duplicator_core instant-max cannot be stacked further`() {
        state.addPassive("duplicator_core")  // instant-max: 5 stacks
        assertEquals(5, state.getPassiveStacks("duplicator_core"))

        // Cannot add more since already at max
        assertFalse(state.addPassive("duplicator_core"))
        assertEquals(5, state.getPassiveStacks("duplicator_core"))
    }

    // ─── Magnet Field ──────────────────────────────────────────

    @Test
    fun `magnet_field adds 30 percent pickup range per stack`() {
        assertEquals(1f, state.pickupRangeMultiplier, 0.001f)

        state.addPassive("magnet_field")
        assertEquals(1.3f, state.pickupRangeMultiplier, 0.001f)

        state.addPassive("magnet_field")
        assertEquals(1.6f, state.pickupRangeMultiplier, 0.001f)

        repeat(3) { state.addPassive("magnet_field") }
        assertEquals(2.5f, state.pickupRangeMultiplier, 0.001f)
    }

    // ─── Phoenix Core ──────────────────────────────────────────

    @Test
    fun `phoenix_core grants exactly 1 extra life regardless of stacks`() {
        assertEquals(0, state.extraLives)

        state.addPassive("phoenix_core")
        assertEquals(1, state.extraLives)

        // Additional stacks don't add more lives
        state.addPassive("phoenix_core")
        assertEquals(1, state.extraLives)

        repeat(3) { state.addPassive("phoenix_core") }
        assertEquals(1, state.extraLives)
    }

    // ─── Extra Weapon Slot ─────────────────────────────────────

    @Test
    fun `extra_weapon_slot enables flag regardless of stacks`() {
        assertFalse(state.hasExtraWeaponSlot)

        state.addPassive("extra_weapon_slot")
        assertTrue(state.hasExtraWeaponSlot)

        // Additional stacks don't change anything
        state.addPassive("extra_weapon_slot")
        assertTrue(state.hasExtraWeaponSlot)
    }

    @Test
    fun `extra_weapon_slot changes slot counts`() {
        assertEquals(4, state.getMaxWeaponSlots())
        assertEquals(4, state.getMaxPassiveSlots())

        state.addPassive("extra_weapon_slot")
        assertEquals(5, state.getMaxWeaponSlots())
        assertEquals(3, state.getMaxPassiveSlots())
    }

    // ─── TB-26 ──────────────────────────────────────────────────

    @Test
    fun `tb26 adds 1 droneCount per stack`() {
        assertEquals(0, state.droneCount)
        assertEquals(false, state.hasDrone)

        state.addPassive("tb26")
        assertEquals(1, state.droneCount)
        assertEquals(true, state.hasDrone)

        state.addPassive("tb26")
        assertEquals(2, state.droneCount)
    }

    @Test
    fun `tb26 maxes at 5 drones`() {
        repeat(5) { state.addPassive("tb26") }
        assertEquals(5, state.droneCount)
        assertEquals(true, state.hasDrone)
    }

    // ─── Combat Drone ───────────────────────────────────────────

    @Test
    fun `combat_drone adds 1 droneCount per stack`() {
        assertEquals(0, state.droneCount)

        state.addPassive("combat_drone")
        assertEquals(1, state.droneCount)
        assertEquals(true, state.hasDrone)

        repeat(4) { state.addPassive("combat_drone") }
        assertEquals(5, state.droneCount)
    }

    // ─── Momentum Drive ────────────────────────────────────────

    @Test
    fun `momentum_drive adds 8 percent damage bonus per stack`() {
        assertEquals(0f, state.momentumDamageBonus, 0.001f)

        state.addPassive("momentum_drive")
        assertEquals(0.08f, state.momentumDamageBonus, 0.001f)

        state.addPassive("momentum_drive")
        assertEquals(0.16f, state.momentumDamageBonus, 0.001f)

        repeat(3) { state.addPassive("momentum_drive") }
        assertEquals(0.40f, state.momentumDamageBonus, 0.001f)
    }

    // ─── Cryo Field ────────────────────────────────────────────

    @Test
    fun `cryo_field applies flat 50 percent slow`() {
        assertEquals(0f, state.cryoSlowPercent, 0.001f)

        state.addPassive("cryo_field")
        assertEquals(0.5f, state.cryoSlowPercent, 0.001f)

        state.addPassive("cryo_field")
        assertEquals(0.5f, state.cryoSlowPercent, 0.001f)  // Flat, does not scale

        repeat(3) { state.addPassive("cryo_field") }
        assertEquals(0.5f, state.cryoSlowPercent, 0.001f)  // Still flat
    }

    @Test
    fun `cryo_field adds 25 percent radius per stack`() {
        assertEquals(1f, state.cryoRadiusMultiplier, 0.001f)

        state.addPassive("cryo_field")
        assertEquals(1.25f, state.cryoRadiusMultiplier, 0.001f)

        state.addPassive("cryo_field")
        assertEquals(1.5f, state.cryoRadiusMultiplier, 0.001f)

        repeat(3) { state.addPassive("cryo_field") }
        assertEquals(2.25f, state.cryoRadiusMultiplier, 0.001f)  // 1.0 + 0.25 * 5
    }

    // ─── Lucky Star ────────────────────────────────────────────

    @Test
    fun `lucky_star is instant-max with 50 percent drop rate and flag`() {
        assertEquals(1f, state.dropRateMultiplier, 0.001f)
        assertFalse(state.hasLuckyStar)

        state.addPassive("lucky_star")  // instant-max: 5 stacks
        assertEquals(5, state.getPassiveStacks("lucky_star"))
        assertEquals(1.50f, state.dropRateMultiplier, 0.001f)
        assertTrue(state.hasLuckyStar)

        // Cannot add more since already at max
        assertFalse(state.addPassive("lucky_star"))
    }

    // ─── Revenge Protocol ──────────────────────────────────────

    @Test
    fun `revenge_protocol stacks are tracked and revengeActive can be toggled`() {
        assertEquals(0, state.passiveStacks["revenge_protocol"] ?: 0)
        assertFalse(state.revengeActive)

        state.addPassive("revenge_protocol")
        assertEquals(1, state.passiveStacks["revenge_protocol"])

        state.addPassive("revenge_protocol")
        assertEquals(2, state.passiveStacks["revenge_protocol"])

        repeat(3) { state.addPassive("revenge_protocol") }
        assertEquals(5, state.passiveStacks["revenge_protocol"])

        // Timer scales with stacks
        val stacks = state.passiveStacks["revenge_protocol"] ?: 0
        state.revengeTimer = stacks * 2f
        state.revengeActive = true
        assertEquals(10f, state.revengeTimer, 0.001f)
    }

    // ─── Vampiric Core ─────────────────────────────────────────

    @Test
    fun `vampiric_core stacks are tracked in passiveStacks`() {
        assertEquals(0, state.passiveStacks["vampiric_core"] ?: 0)

        state.addPassive("vampiric_core")
        assertEquals(1, state.passiveStacks["vampiric_core"])

        state.addPassive("vampiric_core")
        assertEquals(2, state.passiveStacks["vampiric_core"])
    }

    // ─── Glass Cannon ──────────────────────────────────────────

    @Test
    fun `glass_cannon gives instant-max 100 percent damage`() {
        assertEquals(1f, state.damageMultiplier, 0.001f)

        state.addPassive("glass_cannon")  // instant-max: 5 stacks = +100% damage
        assertEquals(2.00f, state.damageMultiplier, 0.001f)
    }

    @Test
    fun `glass_cannon disables shields completely`() {
        assertEquals(Float.MAX_VALUE, state.maxShieldCap, 0.001f)
        assertFalse(state.shieldRegenDisabled)

        state.addPassive("glass_cannon")  // instant-max: shields disabled
        assertEquals(0f, state.maxShieldCap, 0.001f)
        assertTrue(state.shieldRegenDisabled)
    }

    // ─── Multiple Passives Combined ────────────────────────────

    @Test
    fun `multiple different passives combine correctly`() {
        state.addPassive("nano_repair")       // +0.4 regen
        state.addPassive("duplicator_core")   // instant-max: +1 proj (binary effect)
        state.addPassive("glass_cannon")      // instant-max: +100% dmg, shields disabled

        assertEquals(0.4f, state.healthRegen, 0.001f)
        assertEquals(1, state.extraProjectiles)
        assertEquals(2.00f, state.damageMultiplier, 0.001f)  // 1.0 + 1.0
        assertEquals(0f, state.maxShieldCap, 0.001f)
    }

    // ─── Stack Limits ──────────────────────────────────────────

    @Test
    fun `passives cannot exceed max stacks`() {
        repeat(10) { state.addPassive("nano_repair") }

        // Should cap at 5 stacks = 2.0 regen
        assertEquals(2.0f, state.healthRegen, 0.001f)
        assertEquals(5, state.getPassiveStacks("nano_repair"))
    }

    @Test
    fun `addPassive returns false when at max stacks`() {
        repeat(5) {
            assertTrue(state.addPassive("nano_repair"))
        }
        assertFalse(state.addPassive("nano_repair"))
    }

    // ─── Combat Drone → TB-26 Redirect (Astro) ─────────────────

    @Test
    fun `combat_drone pickup redirects to tb26 for Astro`() {
        val state = GameState()
        state.activePilotId = "pilot_astro"
        state.addPassive("tb26")          // Astro starts with 1 tb26 stack
        state.addPassive("combat_drone")  // Should redirect to tb26, not create new slot

        assertEquals(2, state.getPassiveStacks("tb26"))
        assertEquals(0, state.getPassiveStacks("combat_drone"))
    }

    @Test
    fun `combat_drone pickup does NOT redirect for non-Astro pilot`() {
        val state = GameState()
        state.activePilotId = "pilot_medic"
        state.addPassive("combat_drone")

        assertEquals(0, state.getPassiveStacks("tb26"))
        assertEquals(1, state.getPassiveStacks("combat_drone"))
    }

    // ─── Retreat Fields ────────────────────────────────────────

    @Test
    fun `retreatPhase defaults to 0 and resets to 0`() {
        state.retreatPhase = 2
        state.retreatTimer = 3.5f
        state.emergencyShieldActive = true
        state.reset()
        assertEquals(0, state.retreatPhase)
        assertEquals(0f, state.retreatTimer, 0.001f)
        assertFalse(state.emergencyShieldActive)
    }

    // ─── Reset Clears Passives ─────────────────────────────────

    @Test
    fun `reset clears all passive effects`() {
        state.addPassive("nano_repair")
        state.addPassive("glass_cannon")
        state.addPassive("duplicator_core")

        state.reset()

        assertEquals(0f, state.healthRegen, 0.001f)
        assertEquals(1f, state.damageMultiplier, 0.001f)
        assertEquals(0, state.extraProjectiles)
        assertEquals(Float.MAX_VALUE, state.maxShieldCap, 0.001f)
        assertEquals(0, state.getPassiveStacks("nano_repair"))
    }
}

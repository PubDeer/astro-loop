package com.astroloop.game.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for PassiveDefinitions helpers: display names and drone colors.
 */
class PassiveDefinitionsTest {

    @Test
    fun `non-tb26 passive display name unchanged`() {
        assertEquals("Nano Repair", PassiveDefinitions.getDisplayName("nano_repair", "pilot_astro"))
        assertEquals("Nano Repair", PassiveDefinitions.getDisplayName("nano_repair", "pilot_medic"))
    }

    @Test
    fun `lucky_star effect text has comma after upgrades`() {
        val def = PassiveDefinitions.getPassiveDef("lucky_star")
        assertNotNull(def)
        assertTrue(
            "Expected 'upgrades,' but got: ${def!!.effectPerStack}",
            def.effectPerStack.contains("upgrades,")
        )
    }

    @Test
    fun `glass_cannon effect text shows 100 percent`() {
        val def = PassiveDefinitions.getPassiveDef("glass_cannon")
        assertNotNull(def)
        assertTrue(
            "Expected '+100%' but got: ${def!!.effectPerStack}",
            def.effectPerStack.contains("+100%")
        )
    }

    @Test
    fun `tb26 has correct description and effectPerStack`() {
        val def = PassiveDefinitions.getPassiveDef("tb26")
        assertNotNull(def)
        assertEquals("Fly with TB-26", def!!.description)
        assertEquals("+1 drone", def.effectPerStack)
    }

    @Test
    fun `combat_drone exists with correct description and effectPerStack`() {
        val def = PassiveDefinitions.getPassiveDef("combat_drone")
        assertNotNull(def)
        assertEquals("Deploy Combat Drone", def!!.description)
        assertEquals("+1 drone", def.effectPerStack)
    }

    @Test
    fun `getDroneColor returns tb26 blue for astro with tb26 passive`() {
        assertEquals(
            PassiveDefinitions.DRONE_COLOR_TB26,
            PassiveDefinitions.getDroneColor("tb26", "pilot_astro")
        )
    }

    @Test
    fun `getDroneColor returns green for astro with combat_drone passive`() {
        assertEquals(
            PassiveDefinitions.DRONE_COLOR_COMBAT,
            PassiveDefinitions.getDroneColor("combat_drone", "pilot_astro")
        )
    }

    @Test
    fun `getDroneColor returns green for non-astro with any passive`() {
        assertEquals(
            PassiveDefinitions.DRONE_COLOR_COMBAT,
            PassiveDefinitions.getDroneColor("tb26", "pilot_medic")
        )
        assertEquals(
            PassiveDefinitions.DRONE_COLOR_COMBAT,
            PassiveDefinitions.getDroneColor("combat_drone", "pilot_medic")
        )
    }

    @Test
    fun `getDisplayName returns Combat Drone for astro with tb26 in astro loop run`() {
        assertEquals(
            "Combat Drone",
            PassiveDefinitions.getDisplayName("tb26", "pilot_astro", isAstroLoopRun = true)
        )
    }

    @Test
    fun `getDisplayName still returns TB-26 for astro with tb26 in normal run`() {
        assertEquals(
            "TB-26",
            PassiveDefinitions.getDisplayName("tb26", "pilot_astro", isAstroLoopRun = false)
        )
    }

    @Test
    fun `getDroneColor returns green for astro with tb26 in astro loop run`() {
        assertEquals(
            PassiveDefinitions.DRONE_COLOR_COMBAT,
            PassiveDefinitions.getDroneColor("tb26", "pilot_astro", isAstroLoopRun = true)
        )
    }

    @Test
    fun `getDroneColor still returns tb26 blue for astro with tb26 in normal run`() {
        assertEquals(
            PassiveDefinitions.DRONE_COLOR_TB26,
            PassiveDefinitions.getDroneColor("tb26", "pilot_astro", isAstroLoopRun = false)
        )
    }

    @Test
    fun `getEffectivePassiveId returns combat_drone for astro with tb26 in astro loop run`() {
        assertEquals(
            "combat_drone",
            PassiveDefinitions.getEffectivePassiveId("tb26", "pilot_astro", isAstroLoopRun = true)
        )
    }

    @Test
    fun `getEffectivePassiveId returns tb26 for astro in normal run`() {
        assertEquals(
            "tb26",
            PassiveDefinitions.getEffectivePassiveId("tb26", "pilot_astro")
        )
    }

    @Test
    fun `getEffectivePassiveId returns passiveId unchanged for non-astro pilots`() {
        assertEquals(
            "tb26",
            PassiveDefinitions.getEffectivePassiveId("tb26", "pilot_medic", isAstroLoopRun = true)
        )
    }

    @Test
    fun `getEffectivePassiveId returns passiveId unchanged for non-tb26 passives`() {
        assertEquals(
            "nano_repair",
            PassiveDefinitions.getEffectivePassiveId("nano_repair", "pilot_astro", isAstroLoopRun = true)
        )
    }
}

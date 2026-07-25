package com.astroloop.game.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for weapon definitions and evolution lookups.
 */
class WeaponDefinitionsTest {

    // ─── Basic Lookups ─────────────────────────────────────────

    @Test
    fun `getWeaponDef finds base weapons`() {
        val weapon = WeaponDefinitions.getWeaponDef("pulse_cannon")
        assertNotNull(weapon)
        assertEquals("Pulse Cannon", weapon!!.name)
        assertEquals(11f, weapon.baseDamage, 0.001f)
    }

    @Test
    fun `getWeaponDef finds evolution weapons`() {
        val evo = WeaponDefinitions.getWeaponDef("storm_cannon")
        assertNotNull(evo)
        assertEquals("Storm Cannon", evo!!.name)
    }

    @Test
    fun `getWeaponDef returns null for unknown id`() {
        assertNull(WeaponDefinitions.getWeaponDef("nonexistent"))
    }

    // ─── Evolution Lookups ─────────────────────────────────────

    @Test
    fun `getEvolutionFor returns evolution when weapon and passive match`() {
        val evo = WeaponDefinitions.getEvolutionFor("pulse_cannon", "duplicator_core")
        assertNotNull(evo)
        assertEquals("storm_cannon", evo!!.id)
    }

    @Test
    fun `getEvolutionFor returns null when passive does not match`() {
        val evo = WeaponDefinitions.getEvolutionFor("pulse_cannon", "nano_repair")
        assertNull(evo)
    }

    @Test
    fun `getEvolutionFor returns null for unknown weapon`() {
        val evo = WeaponDefinitions.getEvolutionFor("nonexistent", "duplicator_core")
        assertNull(evo)
    }

    // ─── All Evolution Pairs ───────────────────────────────────

    @Test
    fun `pulse_cannon plus duplicator_core equals storm_cannon`() {
        val evo = WeaponDefinitions.getEvolutionFor("pulse_cannon", "duplicator_core")
        assertEquals("storm_cannon", evo?.id)
    }

    @Test
    fun `energy_saw plus momentum_drive equals warp_saw`() {
        val evo = WeaponDefinitions.getEvolutionFor("energy_saw", "momentum_drive")
        assertEquals("warp_saw", evo?.id)
    }

    @Test
    fun `scatter_shot plus vampiric_core equals leech_burst`() {
        val evo = WeaponDefinitions.getEvolutionFor("scatter_shot", "vampiric_core")
        assertEquals("leech_burst", evo?.id)
    }

    @Test
    fun `homing_missiles plus tb26 equals autonomous_ace`() {
        val evo = WeaponDefinitions.getEvolutionFor("homing_missiles", "tb26")
        assertEquals("autonomous_ace", evo?.id)
    }

    @Test
    fun `ion_orbiters plus cryo_field equals frost_ring`() {
        val evo = WeaponDefinitions.getEvolutionFor("ion_orbiters", "cryo_field")
        assertEquals("frost_ring", evo?.id)
    }

    @Test
    fun `railgun plus glass_cannon equals oblivion_beam`() {
        val evo = WeaponDefinitions.getEvolutionFor("railgun", "glass_cannon")
        assertEquals("oblivion_beam", evo?.id)
    }

    @Test
    fun `space_mines plus lucky_star equals jackpot_mines`() {
        val evo = WeaponDefinitions.getEvolutionFor("space_mines", "lucky_star")
        assertEquals("jackpot_mines", evo?.id)
    }

    @Test
    fun `solar_storm plus phoenix_core equals phoenix_flare`() {
        val evo = WeaponDefinitions.getEvolutionFor("solar_storm", "phoenix_core")
        assertEquals("phoenix_flare", evo?.id)
    }

    @Test
    fun `nova_blast plus revenge_protocol equals lingering_nova`() {
        val evo = WeaponDefinitions.getEvolutionFor("nova_blast", "revenge_protocol")
        assertEquals("lingering_nova", evo?.id)
    }

    @Test
    fun `needle_gun plus nano_repair equals siphon_needles`() {
        val evo = WeaponDefinitions.getEvolutionFor("needle_gun", "nano_repair")
        assertEquals("siphon_needles", evo?.id)
    }

    @Test
    fun `cluster_bomb plus magnet_field equals hunter_killer`() {
        val evo = WeaponDefinitions.getEvolutionFor("cluster_bomb", "magnet_field")
        assertEquals("hunter_killer", evo?.id)
    }

    @Test
    fun `flak_cannon plus extra_weapon_slot equals flak_barrage`() {
        val evo = WeaponDefinitions.getEvolutionFor("flak_cannon", "extra_weapon_slot")
        assertEquals("flak_barrage", evo?.id)
    }

    // ─── Evolution Classification ──────────────────────────────

    @Test
    fun `isEvolution returns true for evolution weapons`() {
        assertTrue(WeaponDefinitions.isEvolution("storm_cannon"))
        assertTrue(WeaponDefinitions.isEvolution("warp_saw"))
        assertTrue(WeaponDefinitions.isEvolution("oblivion_beam"))
    }

    @Test
    fun `isEvolution returns false for base weapons`() {
        assertFalse(WeaponDefinitions.isEvolution("pulse_cannon"))
        assertFalse(WeaponDefinitions.isEvolution("railgun"))
    }

    @Test
    fun `isEvolution returns false for unknown weapons`() {
        assertFalse(WeaponDefinitions.isEvolution("nonexistent"))
    }

    // ─── Railgun text + shield behaviour ──────────────────────

    @Test
    fun `railgun description is Piercing sniper shot`() {
        val railgun = WeaponDefinitions.getWeaponDef("railgun")!!
        assertEquals("Piercing sniper shot", railgun.description)
    }

    @Test
    fun `railgun level bonuses describe a widening shot`() {
        val railgun = WeaponDefinitions.getWeaponDef("railgun")!!
        assertEquals(
            listOf(
                "Pierces 10",
                "Wider shot",
                "Wider shot",
                "Wider shot",
                "Beam-wide, MAX"
            ),
            railgun.levelBonuses
        )
    }

    // ─── Weapon Counts ─────────────────────────────────────────

    @Test
    fun `there are 12 base weapons`() {
        assertEquals(12, WeaponDefinitions.weapons.size)
    }

    @Test
    fun `there are 12 evolution weapons`() {
        assertEquals(12, WeaponDefinitions.evolutions.size)
    }

    @Test
    fun `every base weapon has an evolution path`() {
        WeaponDefinitions.weapons.forEach { weapon ->
            assertNotNull("${weapon.id} should have evolutionPassive", weapon.evolutionPassive)
            assertNotNull("${weapon.id} should have evolutionWeaponId", weapon.evolutionWeaponId)
        }
    }

    @Test
    fun `every evolution weapon exists`() {
        WeaponDefinitions.weapons.forEach { weapon ->
            val evoId = weapon.evolutionWeaponId
            val evo = WeaponDefinitions.evolutions.find { it.id == evoId }
            assertNotNull("Evolution $evoId should exist for ${weapon.id}", evo)
        }
    }

    // ─── Energy Saw Level Bonuses ──────────────────────────────

    @Test
    fun `energy saw level bonuses match the growing blade mechanic`() {
        val def = WeaponDefinitions.getWeaponDef("energy_saw")!!
        // EnergySaw has one growing blade (radius, reach, damage scale per level).
        // All levels show grow text.
        assertEquals(
            listOf("1 blade", "Bigger blade", "Bigger blade", "Bigger blade", "Bigger blade, MAX"),
            def.levelBonuses
        )
    }

    @Test
    fun `cluster bomb level bonuses describe bomblets`() {
        val def = WeaponDefinitions.getWeaponDef("cluster_bomb")!!
        assertEquals(
            listOf(
                "2 bomblets",
                "+1 bomblet",
                "+1 bomblet",
                "+1 bomblet",
                "+1 bomblet"
            ),
            def.levelBonuses
        )
    }
}

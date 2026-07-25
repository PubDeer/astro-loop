package com.astroloop.game.system

import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.GameState
import com.astroloop.game.entity.Ship
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for ShipEvolutionSystem.
 * Verifies ship evolution stages and visual updates.
 */
class ShipEvolutionSystemTest {

    private lateinit var evolutionSystem: ShipEvolutionSystem
    private lateinit var ship: Ship
    private lateinit var state: GameState

    @Before
    fun setup() {
        evolutionSystem = ShipEvolutionSystem()
        ship = Ship()
        state = GameState()
        state.reset()
        ship.reset()
    }

    // ─── Evolution Stage Descriptions ────────────────────────────────

    @Test
    fun `stage 0 is Basic Fighter`() {
        assertEquals("Basic Fighter", evolutionSystem.getEvolutionStageDescription(0))
    }

    @Test
    fun `stage 1 is Scout`() {
        assertEquals("Scout", evolutionSystem.getEvolutionStageDescription(1))
    }

    @Test
    fun `stage 2 is Interceptor`() {
        assertEquals("Interceptor", evolutionSystem.getEvolutionStageDescription(2))
    }

    @Test
    fun `stage 3 is Destroyer`() {
        assertEquals("Destroyer", evolutionSystem.getEvolutionStageDescription(3))
    }

    @Test
    fun `stage 4 is Cruiser`() {
        assertEquals("Cruiser", evolutionSystem.getEvolutionStageDescription(4))
    }

    @Test
    fun `stage 5 is Battlecruiser`() {
        assertEquals("Battlecruiser", evolutionSystem.getEvolutionStageDescription(5))
    }

    @Test
    fun `stage 6 is Dreadnought`() {
        assertEquals("Dreadnought", evolutionSystem.getEvolutionStageDescription(6))
    }

    @Test
    fun `invalid stages return Unknown`() {
        assertEquals("Unknown", evolutionSystem.getEvolutionStageDescription(7))
        assertEquals("Unknown", evolutionSystem.getEvolutionStageDescription(-1))
        assertEquals("Unknown", evolutionSystem.getEvolutionStageDescription(100))
    }

    // ─── Ship Visual Updates ─────────────────────────────────────────

    @Test
    fun `updateShipVisuals sets evolution stage from total upgrades`() {
        state.totalUpgradesCollected = 10

        evolutionSystem.updateShipVisuals(ship, state)

        assertEquals(3, ship.evolutionStage)  // 10+ upgrades = stage 3
    }

    @Test
    fun `evolution stage 0 at 0-3 upgrades`() {
        for (upgrades in 0..3) {
            state.totalUpgradesCollected = upgrades
            evolutionSystem.updateShipVisuals(ship, state)
            assertEquals("At $upgrades upgrades", 0, ship.evolutionStage)
        }
    }

    @Test
    fun `evolution stage 1 at 4-6 upgrades`() {
        for (upgrades in 4..6) {
            state.totalUpgradesCollected = upgrades
            evolutionSystem.updateShipVisuals(ship, state)
            assertEquals("At $upgrades upgrades", 1, ship.evolutionStage)
        }
    }

    @Test
    fun `evolution stage 2 at 7-9 upgrades`() {
        for (upgrades in 7..9) {
            state.totalUpgradesCollected = upgrades
            evolutionSystem.updateShipVisuals(ship, state)
            assertEquals("At $upgrades upgrades", 2, ship.evolutionStage)
        }
    }

    @Test
    fun `evolution stage 3 at 10-12 upgrades`() {
        for (upgrades in 10..12) {
            state.totalUpgradesCollected = upgrades
            evolutionSystem.updateShipVisuals(ship, state)
            assertEquals("At $upgrades upgrades", 3, ship.evolutionStage)
        }
    }

    @Test
    fun `evolution stage 4 at 13-15 upgrades`() {
        for (upgrades in 13..15) {
            state.totalUpgradesCollected = upgrades
            evolutionSystem.updateShipVisuals(ship, state)
            assertEquals("At $upgrades upgrades", 4, ship.evolutionStage)
        }
    }

    @Test
    fun `evolution stage 5 at 16-18 upgrades`() {
        for (upgrades in 16..18) {
            state.totalUpgradesCollected = upgrades
            evolutionSystem.updateShipVisuals(ship, state)
            assertEquals("At $upgrades upgrades", 5, ship.evolutionStage)
        }
    }

    @Test
    fun `evolution stage 6 at 19+ upgrades`() {
        for (upgrades in listOf(19, 20, 30, 100)) {
            state.totalUpgradesCollected = upgrades
            evolutionSystem.updateShipVisuals(ship, state)
            assertEquals("At $upgrades upgrades", 6, ship.evolutionStage)
        }
    }

    // ─── Max Health Updates ──────────────────────────────────────────

    @Test
    fun `updateShipVisuals updates max health from multiplier`() {
        state.maxHealthMultiplier = 2f

        evolutionSystem.updateShipVisuals(ship, state)

        assertEquals(GameConfig.SHIP_BASE_HEALTH * 2f, ship.maxHealth, 0.001f)
    }

    @Test
    fun `updateShipVisuals heals difference when max health increases`() {
        ship.maxHealth = 50f
        ship.health = 30f  // Damaged

        state.maxHealthMultiplier = 2f  // Will set maxHealth to 100

        evolutionSystem.updateShipVisuals(ship, state)

        // Should heal by the increase amount (50)
        assertEquals(80f, ship.health, 0.001f)
        assertEquals(100f, ship.maxHealth, 0.001f)
    }

    @Test
    fun `updateShipVisuals does not overheal`() {
        ship.maxHealth = 50f
        ship.health = 50f  // Full health

        state.maxHealthMultiplier = 2f  // Will set maxHealth to 100

        evolutionSystem.updateShipVisuals(ship, state)

        // Should heal by increase, but shouldn't exceed new max
        assertEquals(100f, ship.health, 0.001f)
    }

    @Test
    fun `updateShipVisuals handles health decrease`() {
        ship.maxHealth = 100f
        ship.health = 80f

        state.maxHealthMultiplier = 1f  // Will set maxHealth back to base (50)

        evolutionSystem.updateShipVisuals(ship, state)

        // Health should not increase when max decreases
        assertEquals(80f, ship.health, 0.001f)  // Stays at 80 (over max)
        assertEquals(50f, ship.maxHealth, 0.001f)
    }

    // ─── Weapon Visual Flags ─────────────────────────────────────────

    @Test
    fun `updateShipVisuals sets orbiter flag for ion_orbiters`() {
        state.weaponLevels["ion_orbiters"] = 1

        evolutionSystem.updateShipVisuals(ship, state)

        assertTrue(ship.hasOrbiters)
    }

    @Test
    fun `updateShipVisuals sets missile flag for homing_missiles`() {
        state.weaponLevels["homing_missiles"] = 1

        evolutionSystem.updateShipVisuals(ship, state)

        assertTrue(ship.hasMissiles)
    }

    @Test
    fun `updateShipVisuals clears flags when no matching weapons`() {
        ship.hasOrbiters = true
        ship.hasMissiles = true

        state.weaponLevels.clear()

        evolutionSystem.updateShipVisuals(ship, state)

        assertFalse(ship.hasOrbiters)
        assertFalse(ship.hasMissiles)
    }
}

package com.astroloop.game.system

import com.astroloop.game.core.Camera
import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.GameState
import com.astroloop.game.data.EnemyDefinitions
import com.astroloop.game.entity.EnemyShip
import com.astroloop.game.entity.EntityPool
import com.astroloop.game.entity.Ship
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EnemySpawnSystemTest {

    private lateinit var spawnSystem: EnemySpawnSystem
    private lateinit var state: GameState
    private lateinit var ship: Ship
    private lateinit var camera: Camera

    @Before
    fun setup() {
        val pool = EntityPool(factory = { EnemyShip() }, initialSize = 20)
        spawnSystem = EnemySpawnSystem(pool)
        state = GameState()
        state.reset()
        ship = Ship()
        camera = Camera()
        spawnSystem.unlockedWeaponIds = setOf("pulse_cannon", "energy_saw", "scatter_shot")
        spawnSystem.unlockedPassiveIds = setOf("nano_repair")
    }

    @Test
    fun `energy_saw enemy speed never exceeds 90 percent of player speed`() {
        state.totalUpgradesCollected = 100
        state.survivalTime = 450f

        val sawEnemies = mutableListOf<EnemyShip>()
        repeat(50) {
            val spawned = spawnSystem.update(200f, state, ship, camera, emptyList())
            sawEnemies.addAll(spawned.filter { it.weaponId == "energy_saw" })
        }

        assertTrue("Expected at least one energy_saw enemy to spawn", sawEnemies.isNotEmpty())

        val maxAllowedSpeed = GameConfig.SHIP_BASE_SPEED * 0.9f
        for (enemy in sawEnemies) {
            val def = EnemyDefinitions.getDef(enemy.type)
            val effectiveSpeed = def.baseSpeed * enemy.speedMultiplier
            assertTrue(
                "energy_saw enemy speed $effectiveSpeed exceeds cap $maxAllowedSpeed",
                effectiveSpeed <= maxAllowedSpeed + 0.001f
            )
        }
    }

    @Test
    fun `no enemies spawn in astro loop mode`() {
        state.astroLoopMode = true
        state.survivalTime = 300f
        state.difficultyMultiplier = 2f
        // Use deltaTime large enough to exhaust the spawn timer and trigger spawn attempts
        val spawned = spawnSystem.update(200f, state, ship, camera, emptyList())
        assertTrue("No enemies should spawn in astro loop mode", spawned.isEmpty())
    }

    @Test
    fun `non-saw enemies are not affected by the speed cap`() {
        state.totalUpgradesCollected = 30
        state.survivalTime = 0f

        val spawned = mutableListOf<EnemyShip>()
        repeat(10) {
            spawned.addAll(spawnSystem.update(200f, state, ship, camera, emptyList()))
        }

        val nonSawEnemies = spawned.filter { it.weaponId != "energy_saw" }
        assertTrue("Expected non-saw enemies to spawn", nonSawEnemies.isNotEmpty())

        for (enemy in nonSawEnemies) {
            assertEquals(1.3f, enemy.speedMultiplier, 0.001f)
        }
    }
}

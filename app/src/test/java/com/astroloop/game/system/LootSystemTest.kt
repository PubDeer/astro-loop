package com.astroloop.game.system

import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.GameState
import com.astroloop.game.data.PassiveDefinitions
import com.astroloop.game.data.WeaponDefinitions
import com.astroloop.game.entity.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for LootSystem upgrade spawn gating.
 *
 * The global EntityPools.powerUps singleton is used by LootSystem, so we
 * call freeAll() in @Before to reset state between tests.
 */
class LootSystemTest {

    private lateinit var lootSystem: LootSystem
    private lateinit var state: GameState
    private lateinit var ship: Ship
    private lateinit var spawnSystem: SpawnSystem
    private lateinit var upgradeSystem: UpgradeSystem
    private lateinit var visualEffects: VisualEffectManager

    @Before
    fun setup() {
        // Reset the global pool so prior tests don't bleed state
        EntityPools.powerUps.freeAll()

        state = GameState()
        state.reset()

        ship = Ship()
        spawnSystem = SpawnSystem(EntityPool({ Asteroid() }, 10))
        upgradeSystem = UpgradeSystem(EntityPools.powerUps)
        upgradeSystem.unlockedWeaponIds = WeaponDefinitions.getBaseWeapons().map { it.id }.toSet()
        upgradeSystem.unlockedPassiveIds = PassiveDefinitions.getAllPassives().map { it.id }.toSet()
        visualEffects = VisualEffectManager()

        lootSystem = LootSystem(state, ship, spawnSystem, upgradeSystem, visualEffects)
        lootSystem.unlockedWeaponIds = upgradeSystem.unlockedWeaponIds
        lootSystem.unlockedPassiveIds = upgradeSystem.unlockedPassiveIds
    }

    // ─── Helper: build a fully-maxed GameState ───────────────────────

    /**
     * Fills all 4 weapon slots to L5 and all 4 passive slots to 5 stacks,
     * so isFullyUpgraded() returns true.
     */
    private fun makeFullyUpgradedState(): GameState {
        val s = GameState()
        s.reset()

        val weapons = WeaponDefinitions.getBaseWeapons().take(GameConfig.MAX_WEAPON_SLOTS)
        for (def in weapons) {
            repeat(GameConfig.WEAPON_MAX_LEVEL) { s.addWeapon(def.id) }
        }

        // Exclude instant-max one-time passives (glass_cannon, etc.) so stacks can be set freely
        val instantMaxPassives = setOf("glass_cannon", "phoenix_core", "duplicator_core", "extra_weapon_slot", "lucky_star")
        val passives = PassiveDefinitions.getAllPassives()
            .filter { it.id !in instantMaxPassives }
            .take(GameConfig.MAX_PASSIVE_SLOTS)
        for (def in passives) {
            repeat(GameConfig.PASSIVE_MAX_STACKS) { s.addPassive(def.id) }
        }

        return s
    }

    // ─── Enemy drop: upgrade not spawned when fully upgraded ─────────

    @Test
    fun `enemy drop does not spawn upgrade powerup when fully upgraded`() {
        val fullyMaxed = makeFullyUpgradedState()
        assertTrue("Precondition: state must be fully upgraded", fullyMaxed.isFullyUpgraded())

        // Create a loot system with the fully-maxed state
        val system = LootSystem(fullyMaxed, ship, spawnSystem, upgradeSystem, visualEffects)
        system.unlockedWeaponIds = lootSystem.unlockedWeaponIds
        system.unlockedPassiveIds = lootSystem.unlockedPassiveIds

        // Enemy carrying an upgrade drop
        val enemy = EnemyShip()
        enemy.position.set(500f, 500f)
        enemy.isActive = true
        enemy.dropUpgrades.add("pulse_cannon")

        system.handleEnemyDestroyed(enemy)

        // The only new powerups should be score/yen pickups, not WEAPON or PASSIVE upgrades
        val newUpgradePickups = EntityPools.powerUps.getActiveEntities()
            .filter { it.type == PowerUpType.WEAPON || it.type == PowerUpType.PASSIVE }
        assertEquals(
            "No WEAPON or PASSIVE powerups should spawn when fully upgraded",
            0, newUpgradePickups.size
        )
    }

    // ─── Asteroid drop: upgrade not spawned when fully upgraded ──────

    @Test
    fun `asteroid drop does not spawn upgrade powerup when fully upgraded`() {
        val fullyMaxed = makeFullyUpgradedState()
        assertTrue("Precondition: state must be fully upgraded", fullyMaxed.isFullyUpgraded())

        fullyMaxed.isCorruptionRun = false
        // Ensure cooldown is satisfied so we reach the isFullyUpgraded() guard
        // lastAsteroidUpgradeDropTime defaults to -1000f, survivalTime to 0f — difference is 1000f
        // which exceeds any cooldown, so no extra setup needed.

        val system = LootSystem(fullyMaxed, ship, spawnSystem, upgradeSystem, visualEffects)
        system.unlockedWeaponIds = lootSystem.unlockedWeaponIds
        system.unlockedPassiveIds = lootSystem.unlockedPassiveIds

        val asteroid = Asteroid()
        asteroid.initialize(500f, 500f, AsteroidSize.LARGE, AsteroidType.ROCK)

        system.handleAsteroidDestroyed(asteroid)

        val newUpgradePickups = EntityPools.powerUps.getActiveEntities()
            .filter { it.type == PowerUpType.WEAPON || it.type == PowerUpType.PASSIVE }
        assertEquals(
            "No WEAPON or PASSIVE powerups should spawn when fully upgraded",
            0, newUpgradePickups.size
        )
    }

    // ─── Astro Loop cooldown branching ───────────────────────────────

    @Test
    fun `normal mode uses 12s early cooldown so no drop at 9s survival`() {
        // 9s < ASTEROID_UPGRADE_EARLY_COOLDOWN (12s) → canDrop = false → 0 upgrade drops
        state.astroLoopMode = false
        state.isCorruptionRun = false
        state.survivalTime = 9f
        state.lastAsteroidUpgradeDropTime = 0f   // cooldown gap = 9s < 12s

        val asteroid = Asteroid()
        asteroid.initialize(500f, 500f, AsteroidSize.LARGE, AsteroidType.ROCK)
        lootSystem.handleAsteroidDestroyed(asteroid)

        val upgrades = EntityPools.powerUps.getActiveEntities()
            .filter { it.type == PowerUpType.WEAPON || it.type == PowerUpType.PASSIVE }
        assertEquals("Normal 12s cooldown not satisfied at 9s — no drops expected", 0, upgrades.size)
    }

    @Test
    fun `astro loop uses 14s early cooldown so cooldown is satisfied just past it`() {
        // survivalTime just past ASTRO_LOOP_UPGRADE_EARLY_COOLDOWN (14s) → canDrop = true.
        // Drop chance is now 5% (10% * 0.5 salvage), so we verify the gate opens by running
        // 200 attempts — at 5% each, P(all miss) ≈ 0.003%.
        state.astroLoopMode = true
        state.isCorruptionRun = false
        state.survivalTime = GameConfig.ASTRO_LOOP_UPGRADE_EARLY_COOLDOWN + 1f

        var gotDrop = false
        repeat(200) {
            EntityPools.powerUps.freeAll()
            state.lastAsteroidUpgradeDropTime = 0f
            val asteroid = Asteroid()
            asteroid.initialize(500f, 500f, AsteroidSize.LARGE, AsteroidType.ROCK)
            lootSystem.handleAsteroidDestroyed(asteroid)
            if (EntityPools.powerUps.getActiveEntities()
                    .any { it.type == PowerUpType.WEAPON || it.type == PowerUpType.PASSIVE }) {
                gotDrop = true
            }
        }
        assertTrue("Astro Loop 14s cooldown satisfied just past it — at least one drop expected in 200 tries", gotDrop)
    }

    // ─── Astro Loop evolution diamond drops ───────────────────────

    private fun setupAstroLoopEligibleEvolution() {
        state.astroLoopMode = true
        state.isCorruptionRun = false
        state.hasEvolvedThisGame = false
        state.survivalTime = 500f  // Past the 8-minute gate (480s)
        // pulse_cannon at max level — its evolution passive is duplicator_core
        repeat(GameConfig.WEAPON_MAX_LEVEL) { state.addWeapon("pulse_cannon") }
        state.addPassive("duplicator_core")
    }

    private fun makeAsteroid(): Asteroid {
        val a = Asteroid()
        a.initialize(100f, 100f, AsteroidSize.LARGE, AsteroidType.ROCK)
        return a
    }

    @Test
    fun `astro loop asteroid does not drop evolution diamond before 8 minutes`() {
        setupAstroLoopEligibleEvolution()
        state.survivalTime = 400f  // Before the gate

        val asteroid = makeAsteroid()
        repeat(500) {
            EntityPools.powerUps.freeAll()
            asteroid.isActive = true
            lootSystem.handleAsteroidDestroyed(asteroid)
        }

        val diamonds = EntityPools.powerUps.getActiveEntities()
            .filter { it.type == PowerUpType.EVOLUTION_DIAMOND }
        assertEquals("No diamonds before 8 min", 0, diamonds.size)
    }

    @Test
    fun `astro loop asteroid can drop evolution diamond after 8 minutes`() {
        setupAstroLoopEligibleEvolution()  // survivalTime = 500f

        var diamondDropped = false
        val asteroid = makeAsteroid()
        repeat(500) {
            EntityPools.powerUps.freeAll()
            asteroid.isActive = true
            lootSystem.handleAsteroidDestroyed(asteroid)
            if (EntityPools.powerUps.getActiveEntities()
                    .any { it.type == PowerUpType.EVOLUTION_DIAMOND }) {
                diamondDropped = true
            }
        }

        assertTrue("Evolution diamond should drop sometimes (5% chance, 500 tries)", diamondDropped)
    }

    @Test
    fun `normal mode asteroid never drops evolution diamond`() {
        setupAstroLoopEligibleEvolution()
        state.astroLoopMode = false  // Normal run

        val asteroid = makeAsteroid()
        repeat(500) {
            EntityPools.powerUps.freeAll()
            asteroid.isActive = true
            lootSystem.handleAsteroidDestroyed(asteroid)
        }

        val diamonds = EntityPools.powerUps.getActiveEntities()
            .filter { it.type == PowerUpType.EVOLUTION_DIAMOND }
        assertEquals("Normal mode never drops diamond via asteroid", 0, diamonds.size)
    }

    @Test
    fun `astro loop asteroid does not drop diamond when already evolved`() {
        setupAstroLoopEligibleEvolution()
        state.hasEvolvedThisGame = true

        val asteroid = makeAsteroid()
        repeat(500) {
            EntityPools.powerUps.freeAll()
            asteroid.isActive = true
            lootSystem.handleAsteroidDestroyed(asteroid)
        }

        val diamonds = EntityPools.powerUps.getActiveEntities()
            .filter { it.type == PowerUpType.EVOLUTION_DIAMOND }
        assertEquals("No diamond drop when already evolved", 0, diamonds.size)
    }

    @Test
    fun `astro loop asteroid does not drop diamond during corruption run`() {
        setupAstroLoopEligibleEvolution()
        state.isCorruptionRun = true

        val asteroid = makeAsteroid()
        repeat(500) {
            EntityPools.powerUps.freeAll()
            asteroid.isActive = true
            lootSystem.handleAsteroidDestroyed(asteroid)
        }

        val diamonds = EntityPools.powerUps.getActiveEntities()
            .filter { it.type == PowerUpType.EVOLUTION_DIAMOND }
        assertEquals("No diamond drop during corruption run", 0, diamonds.size)
    }

    // ─── Enemy drop: upgrade DOES spawn when not fully upgraded ──────

    @Test
    fun `enemy drop spawns upgrade powerup when not fully upgraded`() {
        // state starts empty — definitely not fully upgraded
        assertFalse("Precondition: state must NOT be fully upgraded", state.isFullyUpgraded())

        state.isCorruptionRun = false

        val enemy = EnemyShip()
        enemy.position.set(500f, 500f)
        enemy.isActive = true
        enemy.dropUpgrades.add("pulse_cannon")

        lootSystem.handleEnemyDestroyed(enemy)

        val upgradePickups = EntityPools.powerUps.getActiveEntities()
            .filter { it.type == PowerUpType.WEAPON || it.type == PowerUpType.PASSIVE }
        assertEquals(
            "One WEAPON/PASSIVE powerup should spawn when not fully upgraded",
            1, upgradePickups.size
        )
    }
}

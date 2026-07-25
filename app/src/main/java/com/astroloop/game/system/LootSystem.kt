package com.astroloop.game.system

import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.GameState
import com.astroloop.game.data.EnemyDefinitions
import com.astroloop.game.data.PassiveDefinitions
import com.astroloop.game.data.WeaponDefinitions
import com.astroloop.game.entity.*

class LootSystem(
    private val state: GameState,
    private val ship: Ship,
    private val spawnSystem: SpawnSystem,
    private val upgradeSystem: UpgradeSystem,
    private val visualEffects: VisualEffectManager
) {
    var unlockedWeaponIds: Set<String> = emptySet()
    var unlockedPassiveIds: Set<String> = emptySet()

    fun handleAsteroidDestroyed(asteroid: Asteroid) {
        // Small fragments don't drop yen — prevents slowdown after chain explosions
        if (asteroid.size != AsteroidSize.SMALL) {
            val points = when (asteroid.size) {
                AsteroidSize.SMALL -> GameState.POINTS_ASTEROID_SMALL
                AsteroidSize.MEDIUM -> GameState.POINTS_ASTEROID_MEDIUM
                AsteroidSize.LARGE -> GameState.POINTS_ASTEROID_LARGE
            }
            spawnScorePickup(asteroid.position.x, asteroid.position.y, points, fromEnemy = false)
        }

        // Spawn split asteroids
        spawnSystem.spawnSplitAsteroids(asteroid)

        // Suppress upgrade drops in corruption runs or when fully upgraded
        if (!state.isCorruptionRun && !state.isFullyUpgraded()) {
            // Use scaling drop chance and cooldown for early game progression
            val baseDropChance = state.getAsteroidDropChance()
            val dropChance = baseDropChance
            val cooldown = if (state.isEarlyGameDropRate()) {
                if (state.astroLoopMode) GameConfig.ASTRO_LOOP_UPGRADE_EARLY_COOLDOWN else GameConfig.ASTEROID_UPGRADE_EARLY_COOLDOWN
            } else {
                if (state.astroLoopMode) GameConfig.ASTRO_LOOP_UPGRADE_DROP_COOLDOWN else GameConfig.ASTEROID_UPGRADE_DROP_COOLDOWN
            }

            val canDrop = state.survivalTime - state.lastAsteroidUpgradeDropTime >= cooldown
            if (canDrop && kotlin.random.Random.nextFloat() < dropChance) {
                spawnUpgradePowerUp(asteroid.position.x, asteroid.position.y)
                state.lastAsteroidUpgradeDropTime = state.survivalTime
                state.asteroidUpgradesCollected++  // Track for scaling
            }
        }

        // Astro Loop evolution diamond: regular enemies don't spawn in this mode so
        // diamonds can only arrive via asteroid destruction.
        // hasEvolvedThisGame mirrors astroLoopEvolutionUsed (set together in GameSurfaceView);
        // getEligibleEvolutions() also gates on astroLoopEvolutionUsed as belt-and-suspenders.
        if (state.astroLoopMode && !state.isCorruptionRun
            && state.survivalTime >= 480f && !state.hasEvolvedThisGame) {
            val eligible = upgradeSystem.getEligibleEvolutions(state)
            if (eligible.isNotEmpty() && kotlin.random.Random.nextFloat() < 0.05f) {
                val diamond = EntityPools.powerUps.obtain()
                diamond.initializeAsEvolutionDiamond(
                    x = asteroid.position.x,
                    y = asteroid.position.y
                )
            }
        }

        // Volatile asteroids leave a 1-second delayed detonation zone
        if (asteroid.type == AsteroidType.VOLATILE) {
            val detonation = EntityPools.projectiles.obtain()
            detonation.initialize(
                x = asteroid.position.x,
                y = asteroid.position.y,
                vx = 0f,
                vy = 0f,
                projectileType = ProjectileType.PLASMA,
                projectileDamage = 0f,           // No contact damage while waiting
                projectileLifetime = 1.0f        // Detonates when lifetime expires
            )
            detonation.weaponId = "volatile_detonation"
            detonation.explodeOnDeath = true
            detonation.explosionRadius = asteroid.getExplosionRadius()
            detonation.explosionDamage = asteroid.getExplosionDamage()
            detonation.color = 0xFFFF4400.toInt()
            detonation.piercing = true

            // Small flash so the player knows a detonation is pending
            visualEffects.addExplosion(
                asteroid.position.x,
                asteroid.position.y,
                asteroid.getExplosionRadius() * 0.3f,
                0xFFFF4400.toInt()
            )
        }

        // Deactivate original
        asteroid.isActive = false
    }

    fun handleEnemyDestroyed(enemy: EnemyShip) {
        // Award points in normal runs only — no yen in corruption
        if (!state.isCorruptionRun) {
            val points = when (EnemyDefinitions.getTier(enemy.type)) {
                1 -> GameState.POINTS_ENEMY_TIER_1
                2 -> GameState.POINTS_ENEMY_TIER_2
                3 -> GameState.POINTS_ENEMY_TIER_3
                4 -> GameState.POINTS_ENEMY_TIER_4
                else -> GameState.POINTS_ENEMY_TIER_1
            }
            spawnScorePickup(enemy.position.x, enemy.position.y, points, fromEnemy = false)
        }


        // Drop power-ups for each upgrade the enemy was carrying. Skip in corruption, and
        // skip once the player is maxed out — including the early game where nothing else is
        // unlocked yet, so isFullyUpgraded() is never true (see hasAvailableUpgrades()).
        if (!state.isCorruptionRun && hasAvailableUpgrades()) for (upgradeId in enemy.dropUpgrades) {
            val powerUp = EntityPools.powerUps.obtain()
            val isWeapon = PassiveDefinitions.getPassiveDef(upgradeId) == null

            powerUp.initialize(
                x = enemy.position.x + (kotlin.random.Random.nextFloat() - 0.5f) * 30f,
                y = enemy.position.y + (kotlin.random.Random.nextFloat() - 0.5f) * 30f,
                powerUpType = if (isWeapon) PowerUpType.WEAPON else PowerUpType.PASSIVE,
                id = upgradeId
            )
            powerUp.isFromEnemy = true
        }

        // Tier 4 elite diamond drop: 50% chance if player has eligible evolution and hasn't evolved yet (not in corruption)
        if (!state.isCorruptionRun && EnemyDefinitions.getTier(enemy.type) == 4 && !state.hasEvolvedThisGame) {
            val eligible = upgradeSystem.getEligibleEvolutions(state)
            if (eligible.isNotEmpty() && kotlin.random.Random.nextFloat() < 0.5f) {
                val diamond = EntityPools.powerUps.obtain()
                diamond.initializeAsEvolutionDiamond(
                    x = enemy.position.x,
                    y = enemy.position.y
                )
            }
        }

        enemy.isActive = false
    }

    private fun spawnScorePickup(x: Float, y: Float, score: Int, fromEnemy: Boolean) {
        val powerUp = EntityPools.powerUps.obtain()
        powerUp.initializeAsScorePickup(
            x = x + (kotlin.random.Random.nextFloat() - 0.5f) * 15f,
            y = y + (kotlin.random.Random.nextFloat() - 0.5f) * 15f,
            score = score,
            fromEnemy = fromEnemy
        )
    }

    /**
     * True while the player can still pick up or level *something* given what's unlocked.
     * This is broader than [GameState.isFullyUpgraded]: it also reports "maxed" in the early
     * game, where you can't hold 4 weapons/4 passives yet because the rest aren't unlocked,
     * so every slot you *can* fill is already filled and maxed. Drives both new-drop
     * suppression here and the fade-out of pickups already on the field.
     */
    fun hasAvailableUpgrades(): Boolean =
        upgradeableWeaponIds().isNotEmpty() || upgradeablePassiveIds().isNotEmpty()

    private fun upgradeableWeaponIds(): List<String> =
        WeaponDefinitions.getBaseWeapons().filter { def ->
            def.id in unlockedWeaponIds && (
                (state.getWeaponLevel(def.id) > 0 && state.getWeaponLevel(def.id) < GameConfig.WEAPON_MAX_LEVEL) ||
                (state.getWeaponLevel(def.id) == 0 && state.canAddNewWeapon())
            )
        }.map { it.id }

    private fun upgradeablePassiveIds(): List<String> {
        val oneTimePassives = setOf("phoenix_core", "extra_weapon_slot", "glass_cannon", "duplicator_core", "lucky_star")
        return PassiveDefinitions.getAllPassives().filter { def ->
            def.id in unlockedPassiveIds &&
            !(def.id in oneTimePassives && (state.getPassiveStacks(def.id) > 0)) &&
            !(def.id == "extra_weapon_slot" && state.hasExtraWeaponSlot) &&
            (
                (state.getPassiveStacks(def.id) > 0 && state.getPassiveStacks(def.id) < GameConfig.PASSIVE_MAX_STACKS) ||
                (state.getPassiveStacks(def.id) == 0 && state.canAddNewPassive())
            )
        }.map { it.id }
    }

    private fun spawnUpgradePowerUp(x: Float, y: Float) {
        val upgradeableWeapons = upgradeableWeaponIds()
        val upgradeablePassives = upgradeablePassiveIds()

        if (upgradeableWeapons.isEmpty() && upgradeablePassives.isEmpty()) return

        val powerUp = EntityPools.powerUps.obtain()
        val pickWeapon = when {
            upgradeableWeapons.isEmpty() -> false
            upgradeablePassives.isEmpty() -> true
            else -> kotlin.random.Random.nextBoolean()
        }
        val availableIds = if (pickWeapon) upgradeableWeapons else upgradeablePassives
        val randomId = availableIds[kotlin.random.Random.nextInt(availableIds.size)]

        powerUp.initialize(
            x = x + (kotlin.random.Random.nextFloat() - 0.5f) * 20f,
            y = y + (kotlin.random.Random.nextFloat() - 0.5f) * 20f,
            powerUpType = if (pickWeapon) PowerUpType.WEAPON else PowerUpType.PASSIVE,
            id = randomId
        )
    }
}

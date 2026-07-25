package com.astroloop.game.system

import com.astroloop.game.core.Camera
import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.GameState
import com.astroloop.game.data.EnemyDefinitions
import com.astroloop.game.data.PassiveDefinitions
import com.astroloop.game.data.WeaponDefinitions
import com.astroloop.game.entity.EnemyShip
import com.astroloop.game.data.EnemyType
import com.astroloop.game.entity.EntityPool
import com.astroloop.game.entity.Ship
import com.astroloop.game.util.Vector2
import kotlin.math.PI
import kotlin.random.Random

class EnemySpawnSystem(
    private val enemyPool: EntityPool<EnemyShip>
) {
    var unlockedWeaponIds: Set<String> = emptySet()
    var unlockedPassiveIds: Set<String> = emptySet()

    private var spawnTimer: Float = BASE_SPAWN_INTERVAL

    companion object {
        const val BASE_SPAWN_INTERVAL = 100f
        const val MIN_SPAWN_INTERVAL = 35f
        const val INTERVAL_REDUCTION_PER_MIN = 5f  // -5s per 1 minute
    }

    private fun getCurrentSpawnInterval(state: GameState): Float {
        val reductions = (state.survivalTime / 60f).toInt()
        val interval = BASE_SPAWN_INTERVAL - (reductions * INTERVAL_REDUCTION_PER_MIN)
        return interval.coerceAtLeast(MIN_SPAWN_INTERVAL)
    }

    fun update(deltaTime: Float, state: GameState, ship: Ship, camera: Camera, activeEnemies: List<EnemyShip>): List<EnemyShip> {
        if (state.astroLoopMode) return emptyList()
        spawnTimer -= deltaTime
        val spawnedEnemies = mutableListOf<EnemyShip>()

        if (spawnTimer <= 0f) {
            spawnTimer = getCurrentSpawnInterval(state)

            // Check each tier — max 1 enemy per tier on screen at a time
            for (tier in 1..4) {
                val tierTypes = EnemyDefinitions.getTypesForTier(tier)
                val unlocked = tierTypes.filter {
                    EnemyDefinitions.getDef(it).unlockTime <= state.survivalTime
                }
                if (unlocked.isEmpty()) continue

                // Max 1 per tier
                val tierHasActive = activeEnemies.any { it.tier == tier }
                if (tierHasActive) continue

                // Spawn one random type from this tier
                val type = unlocked.random()
                spawnEnemy(type, state, ship, camera)?.let { spawnedEnemies.add(it) }
            }
        }

        return spawnedEnemies
    }

    private fun spawnEnemy(type: EnemyType, state: GameState, ship: Ship, camera: Camera): EnemyShip? {
        val enemy = enemyPool.obtain()
        val def = EnemyDefinitions.getDef(type)

        // Determine spawn position (outside camera view)
        val spawnPos = getSpawnPosition(camera, ship)

        // Determine what upgrades this enemy will drop
        val upgrades = selectUpgradesForEnemy(def.upgradeDropCount, state)

        enemy.initialize(
            x = spawnPos.x,
            y = spawnPos.y,
            enemyType = type,
            upgrades = upgrades
        )

        enemy.spawnShieldDuration = 10f
        enemy.spawnShieldTimer = 10f

        // Scale enemy stats based on player upgrades
        // Health: 10% per upgrade, capped at 3x
        val healthMultiplier = (1f + (state.totalUpgradesCollected * 0.10f)).coerceAtMost(3f)
        enemy.maxHealth *= healthMultiplier
        enemy.health = enemy.maxHealth

        // Damage resistance: 2% per upgrade, capped at 40%
        enemy.damageResistance = (state.totalUpgradesCollected * 0.02f).coerceAtMost(0.4f)

        // Speed: 1% per upgrade, capped at 30%
        enemy.speedMultiplier = (1f + state.totalUpgradesCollected * 0.01f).coerceAtMost(1.3f)

        // Energy saw enemies must always be slower than the player even in charge mode.
        // The Ripper AI multiplies effective speed by 1.5x when charging, so cap at 0.6x
        // of player base speed — charge speed becomes 0.6 * 1.5 = 0.9x player speed.
        if (enemy.weaponId == "energy_saw") {
            enemy.speedMultiplier = enemy.speedMultiplier.coerceAtMost(
                GameConfig.SHIP_BASE_SPEED * 0.6f / def.baseSpeed
            )
        }

        return enemy
    }

    private fun getSpawnPosition(camera: Camera, ship: Ship): Vector2 {
        // Spawn outside camera view, at a fixed distance
        val angle = Random.nextFloat() * 2 * PI.toFloat()
        val distance = GameConfig.ENEMY_SPAWN_DISTANCE

        return Vector2(
            ship.position.x + kotlin.math.cos(angle) * distance,
            ship.position.y + kotlin.math.sin(angle) * distance
        )
    }

    private fun selectUpgradesForEnemy(count: Int, state: GameState): List<String> {
        val upgrades = mutableListOf<String>()

        // Get available (non-maxed) upgrades
        val availableWeapons = WeaponDefinitions.getBaseWeapons()
            .filter { it.id in unlockedWeaponIds && state.getWeaponLevel(it.id) < GameConfig.WEAPON_MAX_LEVEL }
            .map { it.id }
            .toMutableList()

        val availablePassives = PassiveDefinitions.getAllPassives()
            .filter { it.id in unlockedPassiveIds && state.getPassiveStacks(it.id) < GameConfig.PASSIVE_MAX_STACKS }
            .map { it.id }
            .toMutableList()

        // First upgrade is always a weapon if possible
        if (availableWeapons.isNotEmpty()) {
            val weapon = availableWeapons.random()
            upgrades.add(weapon)
            availableWeapons.remove(weapon)
        } else if (availablePassives.isNotEmpty()) {
            val passive = availablePassives.random()
            upgrades.add(passive)
            availablePassives.remove(passive)
        }

        // Additional upgrades can be either
        for (i in 1 until count) {
            val combined = availableWeapons + availablePassives
            if (combined.isEmpty()) break

            val pick = combined.random()
            upgrades.add(pick)

            // Remove from appropriate list
            availableWeapons.remove(pick)
            availablePassives.remove(pick)
        }

        // If nothing available, still spawn but with no drops
        return upgrades
    }

    fun reset() {
        spawnTimer = BASE_SPAWN_INTERVAL
    }
}

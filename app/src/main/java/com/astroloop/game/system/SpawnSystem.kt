package com.astroloop.game.system

import com.astroloop.game.core.Camera
import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.GameState
import com.astroloop.game.entity.*
import com.astroloop.game.util.Vector2
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.random.Random

class SpawnSystem(
    private val asteroidPool: EntityPool<Asteroid>
) {
    private var spawnTimer: Float = 0f
    private var screenWidth: Float = 0f
    private var screenHeight: Float = 0f

    companion object {
        fun asteroidCount(mult: Float): Int = mult.roundToInt().coerceIn(1, 3)

        fun asteroidSpeedFactor(survivalTime: Float, mult: Float): Float {
            val speedMod = 1f + (survivalTime / 60f) * GameConfig.DIFFICULTY_SPEED_INCREASE
            return (speedMod * mult).coerceAtMost(GameConfig.ASTEROID_MAX_SPEED_FACTOR)
        }
    }

    fun initialize(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
        spawnTimer = 0f
    }

    fun update(deltaTime: Float, state: GameState, ship: Ship, camera: Camera? = null): List<Asteroid> {
        spawnTimer -= deltaTime

        val spawnedAsteroids = mutableListOf<Asteroid>()

        if (spawnTimer <= 0f) {
            val spawnRate = getSpawnRate(state)
            spawnTimer = spawnRate

            // Spawn 1-3 asteroids based on difficulty
            val count = asteroidCount(state.difficultyMultiplier)
            repeat(count) {
                val asteroid = spawnAsteroid(state, ship, camera)
                if (asteroid != null) {
                    spawnedAsteroids.add(asteroid)
                }
            }
        }

        return spawnedAsteroids
    }

    private fun getSpawnRate(state: GameState): Float {
        val baseRate = GameConfig.ASTEROID_INITIAL_SPAWN_RATE
        val reduction = state.survivalTime / 60f * GameConfig.DIFFICULTY_SPAWN_RATE_INCREASE
        var rate = (baseRate - reduction).coerceAtLeast(GameConfig.ASTEROID_MIN_SPAWN_RATE)
        // Halve asteroid spawns during corruption run — focus on crew encounters
        if (state.hasCrystalPowers) {
            rate *= 2f
        }
        return rate
    }

    private fun spawnAsteroid(state: GameState, ship: Ship, camera: Camera? = null): Asteroid? {
        val asteroid = asteroidPool.obtain()

        // Determine spawn position (off-screen, relative to camera)
        val spawnPos = getSpawnPosition(ship, camera)

        // Determine asteroid type based on time
        val type = getRandomAsteroidType(state.survivalTime)

        // Determine direction toward ship
        val direction = Vector2(ship.position.x - spawnPos.x, ship.position.y - spawnPos.y).normalize()

        asteroid.initialize(
            x = spawnPos.x,
            y = spawnPos.y,
            asteroidSize = AsteroidSize.LARGE,
            asteroidType = type,
            direction = direction
        )

        // Apply difficulty speed scaling (capped at ASTEROID_MAX_SPEED_FACTOR)
        asteroid.velocity.mul(asteroidSpeedFactor(state.survivalTime, state.difficultyMultiplier))

        return asteroid
    }

    private fun getSpawnPosition(ship: Ship, camera: Camera? = null): Vector2 {
        val margin = GameConfig.ASTEROID_SPAWN_MARGIN

        // If we have a camera, spawn relative to camera view
        if (camera != null) {
            val left = camera.getVisibleLeft()
            val right = camera.getVisibleRight()
            val top = camera.getVisibleTop()
            val bottom = camera.getVisibleBottom()

            // Choose random edge of camera view
            return when (Random.nextInt(4)) {
                0 -> Vector2(left + Random.nextFloat() * screenWidth, top - margin) // Top
                1 -> Vector2(left + Random.nextFloat() * screenWidth, bottom + margin) // Bottom
                2 -> Vector2(left - margin, top + Random.nextFloat() * screenHeight) // Left
                else -> Vector2(right + margin, top + Random.nextFloat() * screenHeight) // Right
            }
        }

        // Fallback: spawn relative to screen (original behavior)
        return when (Random.nextInt(4)) {
            0 -> Vector2(Random.nextFloat() * screenWidth, -margin) // Top
            1 -> Vector2(Random.nextFloat() * screenWidth, screenHeight + margin) // Bottom
            2 -> Vector2(-margin, Random.nextFloat() * screenHeight) // Left
            else -> Vector2(screenWidth + margin, Random.nextFloat() * screenHeight) // Right
        }
    }

    private fun getRandomAsteroidType(survivalTime: Float): AsteroidType {
        val availableTypes = mutableListOf(AsteroidType.ROCK)

        if (survivalTime >= GameConfig.UNLOCK_ICE_ASTEROIDS) {
            availableTypes.add(AsteroidType.ICE)
        }
        if (survivalTime >= GameConfig.UNLOCK_METAL_ASTEROIDS) {
            availableTypes.add(AsteroidType.METAL)
        }
        if (survivalTime >= GameConfig.UNLOCK_VOLATILE_ASTEROIDS) {
            availableTypes.add(AsteroidType.VOLATILE)
        }
        if (survivalTime >= GameConfig.UNLOCK_MAGNETIC_ASTEROIDS) {
            availableTypes.add(AsteroidType.MAGNETIC)
        }
        if (survivalTime >= GameConfig.UNLOCK_TRAIL_ASTEROIDS) {
            availableTypes.add(AsteroidType.TRAIL)
        }

        // Weight toward basic rock type
        val weights = availableTypes.map { type ->
            when (type) {
                AsteroidType.ROCK -> 5
                AsteroidType.ICE -> 3
                AsteroidType.METAL -> 2
                AsteroidType.VOLATILE -> 2
                AsteroidType.MAGNETIC -> 1
                AsteroidType.TRAIL -> 2
            }
        }

        val totalWeight = weights.sum()
        var random = Random.nextInt(totalWeight)

        for ((index, weight) in weights.withIndex()) {
            random -= weight
            if (random < 0) {
                return availableTypes[index]
            }
        }

        return AsteroidType.ROCK
    }

    fun spawnSplitAsteroids(parent: Asteroid): List<Asteroid> {
        if (!parent.shouldSplit()) return emptyList()

        val splitAsteroids = mutableListOf<Asteroid>()
        val count = parent.getSplitCount()
        val nextSize = parent.getNextSize()

        for (i in 0 until count) {
            val asteroid = asteroidPool.obtain()

            // Random direction away from parent center
            val angle = (2 * PI * i / count + Random.nextFloat() * 0.5f).toFloat()
            val direction = Vector2.fromAngle(angle)

            asteroid.initialize(
                x = parent.position.x + direction.x * parent.radius,
                y = parent.position.y + direction.y * parent.radius,
                asteroidSize = nextSize,
                asteroidType = parent.type,
                direction = direction
            )

            // Inherit some of parent velocity
            asteroid.velocity.add(parent.velocity.x * 0.3f, parent.velocity.y * 0.3f)

            // Brief immunity so clip weapons (SolarStorm, NovaBlast) don't instantly destroy children
            asteroid.fragmentImmunityTimer = 0.1f

            splitAsteroids.add(asteroid)
        }

        return splitAsteroids
    }

    fun reset() {
        spawnTimer = GameConfig.ASTEROID_INITIAL_SPAWN_RATE
    }
}

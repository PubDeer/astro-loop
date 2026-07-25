package com.astroloop.game.system

import com.astroloop.game.core.Camera
import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.GameState
import com.astroloop.game.entity.*
import com.astroloop.game.util.Vector2

class MovementSystem {

    private var screenWidth: Float = 0f
    private var screenHeight: Float = 0f
    private var camera: Camera? = null

    fun initialize(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
    }

    fun setCamera(cam: Camera) {
        camera = cam
    }

    fun updateShip(ship: Ship, state: GameState, deltaTime: Float) {
        // Apply speed modifier from passives
        ship.speed = GameConfig.SHIP_BASE_SPEED * state.speedMultiplier

        // Update ship (handles its own movement logic)
        ship.update(deltaTime)

        // Ship can now move freely - camera follows ship
        // No position clamping

        // Health regeneration
        if (state.healthRegen > 0 && ship.health < ship.maxHealth) {
            ship.health = (ship.health + state.healthRegen * deltaTime)
                .coerceAtMost(ship.maxHealth)
        }

    }

    fun updateAsteroids(asteroids: List<Asteroid>, ship: Ship, deltaTime: Float, gameTime: Float = 0f) {
        for (asteroid in asteroids) {
            if (!asteroid.isActive) continue

            // Trail asteroid: record trail points
            if (asteroid.type == AsteroidType.TRAIL) {
                asteroid.updateTrail(gameTime)
            }

            // Tick fragment immunity
            if (asteroid.fragmentImmunityTimer > 0f) {
                asteroid.fragmentImmunityTimer -= deltaTime
            }

            // Special behavior for magnetic asteroids
            if (asteroid.type == AsteroidType.MAGNETIC) {
                val pullStrength = asteroid.getMagneticPullStrength()
                val toShip = Vector2(
                    ship.position.x - asteroid.position.x,
                    ship.position.y - asteroid.position.y
                )
                val dist = toShip.length()
                if (dist > 0 && dist < 600f) {
                    toShip.normalize()
                    asteroid.velocity.add(
                        toShip.x * pullStrength * deltaTime,
                        toShip.y * pullStrength * deltaTime
                    )
                }
            }

            asteroid.update(deltaTime)

            // Infinite world - no wrap around
            // Asteroids will be despawned by camera system when too far
        }
    }

    fun updateProjectiles(projectiles: List<Projectile>, deltaTime: Float) {
        for (projectile in projectiles) {
            if (!projectile.isActive) continue

            projectile.update(deltaTime)

            // With camera system, check against camera bounds or use lifetime
            // Orbiters stay active regardless
            if (projectile.type != ProjectileType.ORBITER) {
                val cam = camera
                if (cam != null) {
                    // Check if projectile is too far from camera center
                    if (cam.isTooFar(projectile.position.x, projectile.position.y, GameConfig.ENTITY_DESPAWN_DISTANCE)) {
                        projectile.isActive = false
                    }
                } else {
                    // Fallback: old screen-based check
                    if (projectile.isOffScreen(screenWidth, screenHeight, 50f)) {
                        projectile.isActive = false
                    }
                }
            }
        }
    }

    fun updatePowerUps(powerUps: List<PowerUp>, deltaTime: Float) {
        for (powerUp in powerUps) {
            if (!powerUp.isActive) continue
            powerUp.update(deltaTime)
        }
    }

    fun applyGravityWellEffects(
        projectiles: List<Projectile>,
        asteroids: List<Asteroid>,
        deltaTime: Float
    ) {
        val gravityWells = projectiles.filter {
            it.isActive && it.type == ProjectileType.GRAVITY
        }

        for (well in gravityWells) {
            // Pull strength is now based on damage (which scales with weapon level)
            // Higher damage = stronger pull
            val pullStrength = well.damage * 100f  // Scale with weapon damage

            for (asteroid in asteroids) {
                if (!asteroid.isActive) continue

                val dx = well.position.x - asteroid.position.x
                val dy = well.position.y - asteroid.position.y
                val distSq = dx * dx + dy * dy
                val dist = kotlin.math.sqrt(distSq)

                if (dist < well.radius && dist > 10f) {
                    // Pull toward center with stronger effect closer to center
                    val distanceFactor = 1f - (dist / well.radius)  // Stronger near center
                    val force = pullStrength * deltaTime * distanceFactor / dist.coerceAtLeast(30f)
                    asteroid.velocity.add(
                        dx / dist * force,
                        dy / dist * force
                    )
                }
            }
        }
    }

    fun applyMineRepulsion(projectiles: List<Projectile>, deltaTime: Float) {
        // Collect all active mines
        val mines = projectiles.filter {
            it.isActive && it.type == ProjectileType.MINE
        }

        if (mines.size < 2) return

        val repulsionStrength = 80f  // Force to push mines apart
        val minDistance = 30f        // Mines try to stay this far apart

        for (i in mines.indices) {
            for (j in i + 1 until mines.size) {
                val mine1 = mines[i]
                val mine2 = mines[j]

                val dx = mine2.position.x - mine1.position.x
                val dy = mine2.position.y - mine1.position.y
                val distSq = dx * dx + dy * dy
                val dist = kotlin.math.sqrt(distSq)

                if (dist < minDistance && dist > 0.1f) {
                    // Calculate repulsion force
                    val overlap = minDistance - dist
                    val force = (overlap / minDistance) * repulsionStrength * deltaTime
                    val nx = dx / dist
                    val ny = dy / dist

                    // Push mines apart equally
                    mine1.velocity.x -= nx * force
                    mine1.velocity.y -= ny * force
                    mine2.velocity.x += nx * force
                    mine2.velocity.y += ny * force
                }
            }
        }

        // Apply gentle drag to mines so they settle down
        for (mine in mines) {
            mine.velocity.x *= 0.98f
            mine.velocity.y *= 0.98f
        }
    }
}

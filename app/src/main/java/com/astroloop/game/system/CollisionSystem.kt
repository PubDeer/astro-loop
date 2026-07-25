package com.astroloop.game.system

import com.astroloop.game.core.GameConfig
import com.astroloop.game.entity.*
import com.astroloop.game.util.SpatialHash
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class CollisionResult(
    val asteroidHits: List<Pair<Projectile, Asteroid>>,
    val shipHit: Asteroid?,
    val powerUpCollected: PowerUp?,              // Upgrade power-up (opens selection screen)
    val evolutionDiamondCollected: PowerUp?,     // Evolution diamond (opens evolution selection)
    val scorePickupsCollected: List<PowerUp>,    // Score pickups (immediately add points)
    val explosions: List<ExplosionEvent>,
    val lightningForks: List<LightningForkEvent>  // Lightning fork events
)

data class LightningForkEvent(
    val x: Float,
    val y: Float,
    val forkCount: Int,
    val forkRange: Float,
    val damage: Float,
    val hitEntity: Entity  // The entity that was hit (exclude from fork targets)
)

data class ExplosionEvent(
    val x: Float,
    val y: Float,
    val radius: Float,
    val damage: Float,
    val sourceWeaponId: String,
    val color: Int
)

data class TrailHits(
    var shipDamage: Float = 0f,
    val enemyHits: MutableList<Pair<EnemyShip, Float>> = mutableListOf()
)

class CollisionSystem {

    private val spatialHash = SpatialHash(100f)
    private val trailDamageCooldowns: MutableMap<Entity, Float> = mutableMapOf()
    private val trailDamageCooldown = 0.5f  // Damage tick every 0.5s

    fun updateTrailCooldowns(deltaTime: Float) {
        val iterator = trailDamageCooldowns.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            entry.setValue(entry.value - deltaTime)
            if (entry.value <= 0f) iterator.remove()
        }
    }

    fun checkTrailCollisions(
        asteroids: List<Asteroid>,
        ship: Ship,
        enemies: List<EnemyShip>
    ): TrailHits {
        val result = TrailHits()

        for (asteroid in asteroids) {
            if (!asteroid.isActive || asteroid.type != AsteroidType.TRAIL) continue
            val trailWidth = asteroid.getTrailWidth()
            val trailDamage = asteroid.getTrailDamage()

            // Check ship
            if (!trailDamageCooldowns.containsKey(ship)) {
                for (point in asteroid.trailPoints) {
                    val dx = ship.position.x - point.x
                    val dy = ship.position.y - point.y
                    if (dx * dx + dy * dy < (trailWidth + ship.radius) * (trailWidth + ship.radius)) {
                        result.shipDamage = trailDamage
                        trailDamageCooldowns[ship] = trailDamageCooldown
                        break
                    }
                }
            }

            // Check enemies
            for (enemy in enemies) {
                if (!enemy.isActive || trailDamageCooldowns.containsKey(enemy)) continue
                for (point in asteroid.trailPoints) {
                    val dx = enemy.position.x - point.x
                    val dy = enemy.position.y - point.y
                    if (dx * dx + dy * dy < (trailWidth + enemy.radius) * (trailWidth + enemy.radius)) {
                        result.enemyHits.add(Pair(enemy, trailDamage))
                        trailDamageCooldowns[enemy] = trailDamageCooldown
                        break
                    }
                }
            }
        }
        return result
    }

    fun checkCollisions(
        ship: Ship,
        asteroids: List<Asteroid>,
        projectiles: List<Projectile>,
        powerUps: List<PowerUp>,
        pickupRange: Float,
        pullSpeed: Float
    ): CollisionResult {
        val asteroidHits = mutableListOf<Pair<Projectile, Asteroid>>()
        val scorePickupsCollected = mutableListOf<PowerUp>()
        val explosions = mutableListOf<ExplosionEvent>()
        val lightningForks = mutableListOf<LightningForkEvent>()

        // Rebuild spatial hash
        spatialHash.clear()
        asteroids.filter { it.isActive }.forEach { spatialHash.insert(it) }

        processProjectileAsteroidHits(projectiles, asteroidHits, explosions, lightningForks)
        val shipHit = processShipAsteroidHit(ship)
        val (powerUpCollected, evolutionDiamondCollected) = processPickupCollection(ship, powerUps, pickupRange, pullSpeed, scorePickupsCollected)

        return CollisionResult(asteroidHits, shipHit, powerUpCollected, evolutionDiamondCollected, scorePickupsCollected, explosions, lightningForks)
    }

    private fun processProjectileAsteroidHits(
        projectiles: List<Projectile>,
        asteroidHits: MutableList<Pair<Projectile, Asteroid>>,
        explosions: MutableList<ExplosionEvent>,
        lightningForks: MutableList<LightningForkEvent>
    ) {
        for (projectile in projectiles) {
            if (!projectile.isActive) continue
            if (projectile.isVisualOnly) continue
            if (projectile.type == ProjectileType.LIGHTNING && projectile.bounceCount == 99) continue

            val isBeam = projectile.type == ProjectileType.BEAM || projectile.type == ProjectileType.PLASMA
            val queryRadius = if (isBeam) projectile.length + 100f else projectile.radius + 100f

            val nearby = spatialHash.query(
                projectile.position.x,
                projectile.position.y,
                queryRadius
            )

            for (entity in nearby) {
                val asteroid = entity as? Asteroid ?: continue
                if (!asteroid.isActive) continue

                val collides = if (isBeam) {
                    checkBeamCollision(projectile, asteroid)
                } else {
                    projectile.collidesWith(asteroid)
                }

                if (collides) {
                    if (projectile.onHit(asteroid)) {
                        asteroidHits.add(Pair(projectile, asteroid))

                        if (projectile.type == ProjectileType.LIGHTNING && projectile.bounceCount == 0) {
                            lightningForks.add(LightningForkEvent(
                                x = projectile.position.x,
                                y = projectile.position.y,
                                forkCount = projectile.maxBounces,
                                forkRange = projectile.explosionRadius,
                                damage = projectile.damage,
                                hitEntity = asteroid
                            ))
                            projectile.bounceCount = 1
                            projectile.isActive = false
                        }

                        if (projectile.explodeOnDeath && !projectile.isActive && projectile.type != ProjectileType.LIGHTNING) {
                            explosions.add(ExplosionEvent(
                                x = projectile.position.x,
                                y = projectile.position.y,
                                radius = projectile.explosionRadius,
                                damage = projectile.explosionDamage,
                                sourceWeaponId = projectile.weaponId,
                                color = projectile.color
                            ))
                        }
                    }

                    if (!projectile.isActive) break
                }
            }
        }
    }

    private fun processShipAsteroidHit(ship: Ship): Asteroid? {
        if (ship.isInvulnerable) return null

        val nearShip = spatialHash.query(
            ship.position.x,
            ship.position.y,
            ship.radius + 100f
        )

        for (entity in nearShip) {
            val asteroid = entity as? Asteroid ?: continue
            if (!asteroid.isActive) continue

            if (ship.collidesWith(asteroid)) {
                return asteroid
            }
        }
        return null
    }

    private fun processPickupCollection(
        ship: Ship,
        powerUps: List<PowerUp>,
        pickupRange: Float,
        pullSpeed: Float,
        scorePickupsCollected: MutableList<PowerUp>
    ): Pair<PowerUp?, PowerUp?> {
        var powerUpCollected: PowerUp? = null
        var evolutionDiamondCollected: PowerUp? = null

        for (powerUp in powerUps) {
            if (!powerUp.isActive) continue

            val dist = ship.position.distance(powerUp.position)

            if (dist <= pickupRange) {
                powerUp.isBeingPulled = true
                powerUp.moveToward(ship.position.x, ship.position.y, pullSpeed, 0.016f)
            }

            if (dist <= GameConfig.POWERUP_COLLECT_RANGE) {
                when (powerUp.type) {
                    PowerUpType.SCORE_PICKUP -> {
                        scorePickupsCollected.add(powerUp)
                        powerUp.isActive = false
                    }
                    PowerUpType.EVOLUTION_DIAMOND -> {
                        if (evolutionDiamondCollected == null && powerUpCollected == null) {
                            evolutionDiamondCollected = powerUp
                            powerUp.isActive = false
                        }
                    }
                    else -> {
                        if (powerUpCollected == null && evolutionDiamondCollected == null) {
                            powerUpCollected = powerUp
                            powerUp.isActive = false
                        }
                    }
                }
            }
        }

        return Pair(powerUpCollected, evolutionDiamondCollected)
    }

    // Find nearby targets for lightning forks (excluding already hit entity)
    fun findForkTargets(
        x: Float,
        y: Float,
        range: Float,
        count: Int,
        exclude: Entity,
        asteroids: List<Asteroid>
    ): List<Asteroid> {
        return asteroids
            .filter { it.isActive && it != exclude }
            .map { asteroid ->
                val dx = asteroid.position.x - x
                val dy = asteroid.position.y - y
                val dist = sqrt(dx * dx + dy * dy)
                Pair(asteroid, dist)
            }
            .filter { it.second <= range }
            .sortedBy { it.second }
            .take(count)
            .map { it.first }
    }

    fun processExplosions(
        explosions: List<ExplosionEvent>,
        asteroids: List<Asteroid>
    ): List<Pair<ExplosionEvent, Asteroid>> {
        val hits = mutableListOf<Pair<ExplosionEvent, Asteroid>>()

        for (explosion in explosions) {
            for (asteroid in asteroids) {
                if (!asteroid.isActive) continue

                val dist = sqrt(
                    (explosion.x - asteroid.position.x) * (explosion.x - asteroid.position.x) +
                    (explosion.y - asteroid.position.y) * (explosion.y - asteroid.position.y)
                )

                if (dist <= explosion.radius + asteroid.radius) {
                    hits.add(Pair(explosion, asteroid))
                }
            }
        }

        return hits
    }

    // Check if point is within any asteroid (for gravity wells, etc.)
    fun getAsteroidsInRadius(
        x: Float,
        y: Float,
        radius: Float,
        asteroids: List<Asteroid>
    ): List<Asteroid> {
        return asteroids.filter { asteroid ->
            if (!asteroid.isActive) return@filter false
            val dist = sqrt(
                (x - asteroid.position.x) * (x - asteroid.position.x) +
                (y - asteroid.position.y) * (y - asteroid.position.y)
            )
            dist <= radius + asteroid.radius
        }
    }

    // Check if point is within any enemy ship
    fun getEnemiesInRadius(
        x: Float,
        y: Float,
        radius: Float,
        enemies: List<EnemyShip>
    ): List<EnemyShip> {
        return enemies.filter { enemy ->
            if (!enemy.isActive) return@filter false
            val dx = enemy.position.x - x
            val dy = enemy.position.y - y
            val distSq = dx * dx + dy * dy
            distSq <= (radius + enemy.radius) * (radius + enemy.radius)
        }
    }

    // Find the nearest valid target for chain lightning bouncing
    private fun findNearestTarget(
        projectile: Projectile,
        asteroids: List<Asteroid>,
        alreadyHit: Set<Entity>
    ): Asteroid? {
        val maxBounceRange = 300f  // Maximum distance to bounce to next target

        return asteroids
            .filter { it.isActive && !alreadyHit.contains(it) }
            .minByOrNull { asteroid ->
                val dx = asteroid.position.x - projectile.position.x
                val dy = asteroid.position.y - projectile.position.y
                dx * dx + dy * dy
            }
            ?.takeIf { asteroid ->
                val dx = asteroid.position.x - projectile.position.x
                val dy = asteroid.position.y - projectile.position.y
                sqrt(dx * dx + dy * dy) <= maxBounceRange
            }
    }

    // Redirect projectile toward a new target
    private fun redirectToTarget(projectile: Projectile, target: Asteroid) {
        val dx = target.position.x - projectile.position.x
        val dy = target.position.y - projectile.position.y
        val dist = sqrt(dx * dx + dy * dy)

        if (dist > 0) {
            val speed = projectile.velocity.length()
            projectile.velocity.set(dx / dist * speed, dy / dist * speed)
            // Keep projectile active for bouncing
            projectile.isActive = true
        }
    }

    // Check beam collision using line segment vs circle
    private fun checkBeamCollision(beam: Projectile, target: Entity): Boolean {
        // Standard beam: extends from position in direction of beamAngle
        val startX = beam.position.x
        val startY = beam.position.y
        val beamAngle = beam.beamAngle
        val beamLength = beam.length
        val beamHalfWidth = beam.radius  // Radius is used as half-width for beams

        // End point of beam
        val endX = startX + cos(beamAngle) * beamLength
        val endY = startY + sin(beamAngle) * beamLength

        // Check distance from target center to line segment
        val distToLine = pointToLineSegmentDistance(
            target.position.x, target.position.y,
            startX, startY, endX, endY
        )

        // Collision if distance is less than beam half-width + target radius
        return distToLine <= beamHalfWidth + target.radius
    }

    /**
     * Check if a line segment intersects a circle (e.g. beam vs entity).
     * Returns true if segment (startX,startY)→(endX,endY) intersects circle at (cx,cy) with given radius.
     */
    fun checkLineCircleIntersection(
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        cx: Float, cy: Float,
        radius: Float
    ): Boolean {
        val dx = endX - startX
        val dy = endY - startY
        val fx = startX - cx
        val fy = startY - cy

        val a = dx * dx + dy * dy
        if (a == 0f) return false
        val b = 2f * (fx * dx + fy * dy)
        val c = fx * fx + fy * fy - radius * radius

        val discriminant = b * b - 4f * a * c
        if (discriminant < 0) return false

        val t1 = (-b - sqrt(discriminant)) / (2f * a)
        val t2 = (-b + sqrt(discriminant)) / (2f * a)
        return (t1 in 0f..1f) || (t2 in 0f..1f) || (t1 < 0f && t2 > 1f)
    }

    fun checkEnemyAsteroidCollisions(enemies: List<EnemyShip>, asteroids: List<Asteroid>): List<EnemyShip> {
        val destroyedEnemies = mutableListOf<EnemyShip>()

        for (enemy in enemies) {
            if (!enemy.isActive) continue

            val nearby = spatialHash.query(
                enemy.position.x,
                enemy.position.y,
                enemy.radius + GameConfig.ASTEROID_LARGE_SIZE
            )

            for (entity in nearby) {
                val asteroid = entity as? Asteroid ?: continue
                if (!asteroid.isActive) continue

                val dx = enemy.position.x - asteroid.position.x
                val dy = enemy.position.y - asteroid.position.y
                val dist = sqrt(dx * dx + dy * dy)
                val collisionDist = enemy.radius + asteroid.radius

                if (dist < collisionDist) {
                    if (enemy.takeDamage(asteroid.damage)) {
                        destroyedEnemies.add(enemy)
                    }
                    break
                }
            }
        }

        return destroyedEnemies
    }

    // Calculate distance from point to line segment
    private fun pointToLineSegmentDistance(
        px: Float, py: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float
    ): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        val lengthSq = dx * dx + dy * dy

        if (lengthSq == 0f) {
            // Line segment is a point
            return sqrt((px - x1) * (px - x1) + (py - y1) * (py - y1))
        }

        // Calculate projection of point onto line
        val t = ((px - x1) * dx + (py - y1) * dy) / lengthSq
        val clampedT = t.coerceIn(0f, 1f)

        // Closest point on segment
        val closestX = x1 + clampedT * dx
        val closestY = y1 + clampedT * dy

        // Distance from point to closest point on segment
        return sqrt((px - closestX) * (px - closestX) + (py - closestY) * (py - closestY))
    }
}

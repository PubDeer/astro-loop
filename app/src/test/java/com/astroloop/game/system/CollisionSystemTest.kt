package com.astroloop.game.system

import com.astroloop.game.entity.*
import com.astroloop.game.util.Vector2
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * Tests for CollisionSystem, focusing on:
 * - Beam collision detection (checkBeamCollision, pointToLineSegmentDistance)
 * - Fork target finding for chain lightning
 * - Explosion processing
 * - Radius queries for asteroids and enemies
 */
class CollisionSystemTest {

    private lateinit var collisionSystem: CollisionSystem

    @Before
    fun setup() {
        collisionSystem = CollisionSystem()
    }

    // ─── Point to Line Segment Distance Tests ───────────────────────

    @Test
    fun `pointToLineSegmentDistance returns zero when point is on line`() {
        // Point exactly on the line segment
        val distance = invokePointToLineSegmentDistance(
            px = 5f, py = 0f,
            x1 = 0f, y1 = 0f,
            x2 = 10f, y2 = 0f
        )
        assertEquals(0f, distance, 0.001f)
    }

    @Test
    fun `pointToLineSegmentDistance returns perpendicular distance for point above line`() {
        // Point 5 units above a horizontal line
        val distance = invokePointToLineSegmentDistance(
            px = 5f, py = 5f,
            x1 = 0f, y1 = 0f,
            x2 = 10f, y2 = 0f
        )
        assertEquals(5f, distance, 0.001f)
    }

    @Test
    fun `pointToLineSegmentDistance returns distance to nearest endpoint when past segment`() {
        // Point beyond the end of the segment (should snap to endpoint)
        val distance = invokePointToLineSegmentDistance(
            px = 15f, py = 0f,
            x1 = 0f, y1 = 0f,
            x2 = 10f, y2 = 0f
        )
        assertEquals(5f, distance, 0.001f)
    }

    @Test
    fun `pointToLineSegmentDistance returns distance to start when before segment`() {
        // Point before the start of the segment
        val distance = invokePointToLineSegmentDistance(
            px = -3f, py = 4f,
            x1 = 0f, y1 = 0f,
            x2 = 10f, y2 = 0f
        )
        // Distance from (-3, 4) to (0, 0) = sqrt(9 + 16) = 5
        assertEquals(5f, distance, 0.001f)
    }

    @Test
    fun `pointToLineSegmentDistance handles zero-length segment`() {
        // Degenerate case: segment is a point
        val distance = invokePointToLineSegmentDistance(
            px = 3f, py = 4f,
            x1 = 0f, y1 = 0f,
            x2 = 0f, y2 = 0f
        )
        // Distance from (3, 4) to (0, 0) = 5
        assertEquals(5f, distance, 0.001f)
    }

    @Test
    fun `pointToLineSegmentDistance works with diagonal line`() {
        // 45 degree line from origin
        val distance = invokePointToLineSegmentDistance(
            px = 0f, py = 10f,
            x1 = 0f, y1 = 0f,
            x2 = 10f, y2 = 10f
        )
        // Perpendicular distance to y=x line from (0, 10)
        // Distance = |10 - 0| / sqrt(2) = 10 / sqrt(2) ~ 7.071
        assertEquals(7.071f, distance, 0.01f)
    }

    // ─── Beam Collision Tests ───────────────────────────────────────

    @Test
    fun `beam collision detects asteroid on beam path`() {
        val beam = createBeamProjectile(
            x = 0f, y = 0f,
            angle = 0f,  // Pointing right
            length = 200f,
            halfWidth = 5f
        )

        val asteroid = Asteroid()
        asteroid.position.set(100f, 0f)
        asteroid.radius = 20f
        asteroid.isActive = true

        val collides = invokeCheckBeamCollision(beam, asteroid)
        assertTrue("Asteroid directly in beam path should collide", collides)
    }

    @Test
    fun `beam collision detects asteroid near beam edge`() {
        val beam = createBeamProjectile(
            x = 0f, y = 0f,
            angle = 0f,
            length = 200f,
            halfWidth = 10f
        )

        val asteroid = Asteroid()
        asteroid.position.set(100f, 15f)  // 15 units above center
        asteroid.radius = 10f  // Combined with halfWidth = 20, point is 15 away
        asteroid.isActive = true

        val collides = invokeCheckBeamCollision(beam, asteroid)
        assertTrue("Asteroid within beam width + radius should collide", collides)
    }

    @Test
    fun `beam collision misses asteroid outside beam`() {
        val beam = createBeamProjectile(
            x = 0f, y = 0f,
            angle = 0f,
            length = 200f,
            halfWidth = 5f
        )

        val asteroid = Asteroid()
        asteroid.position.set(100f, 50f)  // 50 units above, way outside
        asteroid.radius = 10f
        asteroid.isActive = true

        val collides = invokeCheckBeamCollision(beam, asteroid)
        assertFalse("Asteroid far from beam should not collide", collides)
    }

    @Test
    fun `beam collision misses asteroid beyond beam length`() {
        val beam = createBeamProjectile(
            x = 0f, y = 0f,
            angle = 0f,
            length = 100f,
            halfWidth = 5f
        )

        val asteroid = Asteroid()
        asteroid.position.set(200f, 0f)  // Beyond beam end
        asteroid.radius = 10f
        asteroid.isActive = true

        val collides = invokeCheckBeamCollision(beam, asteroid)
        assertFalse("Asteroid beyond beam length should not collide", collides)
    }

    @Test
    fun `beam collision works with angled beam`() {
        val beam = createBeamProjectile(
            x = 0f, y = 0f,
            angle = (PI / 4).toFloat(),  // 45 degrees
            length = 141.4f,  // sqrt(2) * 100 to reach (100, 100)
            halfWidth = 10f
        )

        val asteroid = Asteroid()
        asteroid.position.set(50f, 50f)  // On the 45 degree line
        asteroid.radius = 15f
        asteroid.isActive = true

        val collides = invokeCheckBeamCollision(beam, asteroid)
        assertTrue("Asteroid on angled beam path should collide", collides)
    }

    // ─── Fork Target Finding Tests ───────────────────────────────────

    @Test
    fun `findForkTargets returns closest asteroids within range`() {
        val asteroids = listOf(
            createAsteroidAt(100f, 0f),   // Distance 100
            createAsteroidAt(50f, 0f),    // Distance 50
            createAsteroidAt(200f, 0f),   // Distance 200
            createAsteroidAt(0f, 75f)     // Distance 75
        )

        val targets = collisionSystem.findForkTargets(
            x = 0f, y = 0f,
            range = 150f,
            count = 2,
            exclude = asteroids[0],  // Exclude the first one
            asteroids = asteroids
        )

        assertEquals(2, targets.size)
        // Should get the 50 and 75 distance ones (sorted by distance)
        assertEquals(50f, targets[0].position.x, 0.001f)  // Closest
        assertEquals(75f, targets[1].position.y, 0.001f)  // Second closest
    }

    @Test
    fun `findForkTargets excludes the specified entity`() {
        val excludeTarget = createAsteroidAt(10f, 0f)
        val asteroids = listOf(
            excludeTarget,
            createAsteroidAt(50f, 0f),
            createAsteroidAt(100f, 0f)
        )

        val targets = collisionSystem.findForkTargets(
            x = 0f, y = 0f,
            range = 200f,
            count = 5,
            exclude = excludeTarget,
            asteroids = asteroids
        )

        assertEquals(2, targets.size)
        assertFalse(targets.contains(excludeTarget))
    }

    @Test
    fun `findForkTargets respects range limit`() {
        val asteroids = listOf(
            createAsteroidAt(50f, 0f),
            createAsteroidAt(150f, 0f),  // Outside range
            createAsteroidAt(200f, 0f)   // Outside range
        )

        val targets = collisionSystem.findForkTargets(
            x = 0f, y = 0f,
            range = 100f,
            count = 10,
            exclude = asteroids[2],
            asteroids = asteroids
        )

        assertEquals(1, targets.size)
        assertEquals(50f, targets[0].position.x, 0.001f)
    }

    @Test
    fun `findForkTargets respects count limit`() {
        val asteroids = (1..10).map { createAsteroidAt(it * 10f, 0f) }

        val targets = collisionSystem.findForkTargets(
            x = 0f, y = 0f,
            range = 200f,
            count = 3,
            exclude = asteroids[9],
            asteroids = asteroids
        )

        assertEquals(3, targets.size)
    }

    @Test
    fun `findForkTargets excludes inactive asteroids`() {
        val asteroids = listOf(
            createAsteroidAt(10f, 0f).apply { isActive = false },
            createAsteroidAt(50f, 0f)
        )

        val targets = collisionSystem.findForkTargets(
            x = 0f, y = 0f,
            range = 100f,
            count = 10,
            exclude = asteroids[1],
            asteroids = asteroids
        )

        assertTrue(targets.isEmpty())
    }

    // ─── Explosion Processing Tests ──────────────────────────────────

    @Test
    fun `processExplosions detects asteroids in explosion radius`() {
        val explosions = listOf(
            ExplosionEvent(x = 0f, y = 0f, radius = 100f, damage = 50f, sourceWeaponId = "test", color = 0xFFFFFFFF.toInt())
        )

        val asteroids = listOf(
            createAsteroidAt(50f, 0f, radius = 20f),   // Inside
            createAsteroidAt(200f, 0f, radius = 20f)  // Outside
        )

        val hits = collisionSystem.processExplosions(explosions, asteroids)

        assertEquals(1, hits.size)
        assertEquals(50f, hits[0].second.position.x, 0.001f)
    }

    @Test
    fun `processExplosions accounts for asteroid radius in hit detection`() {
        val explosions = listOf(
            ExplosionEvent(x = 0f, y = 0f, radius = 50f, damage = 50f, sourceWeaponId = "test", color = 0xFFFFFFFF.toInt())
        )

        val asteroids = listOf(
            createAsteroidAt(80f, 0f, radius = 40f)  // Center at 80, radius 40, edge at 40 from origin
        )

        val hits = collisionSystem.processExplosions(explosions, asteroids)

        assertEquals(1, hits.size)  // 50 + 40 = 90 > 80
    }

    @Test
    fun `processExplosions ignores inactive asteroids`() {
        val explosions = listOf(
            ExplosionEvent(x = 0f, y = 0f, radius = 100f, damage = 50f, sourceWeaponId = "test", color = 0xFFFFFFFF.toInt())
        )

        val asteroids = listOf(
            createAsteroidAt(50f, 0f).apply { isActive = false }
        )

        val hits = collisionSystem.processExplosions(explosions, asteroids)

        assertTrue(hits.isEmpty())
    }

    @Test
    fun `processExplosions handles multiple explosions`() {
        val explosions = listOf(
            ExplosionEvent(x = 0f, y = 0f, radius = 50f, damage = 50f, sourceWeaponId = "test1", color = 0xFFFFFFFF.toInt()),
            ExplosionEvent(x = 100f, y = 0f, radius = 50f, damage = 50f, sourceWeaponId = "test2", color = 0xFFFFFFFF.toInt())
        )

        val asteroids = listOf(
            createAsteroidAt(25f, 0f, radius = 10f),   // Hit by first explosion
            createAsteroidAt(125f, 0f, radius = 10f)  // Hit by second explosion
        )

        val hits = collisionSystem.processExplosions(explosions, asteroids)

        assertEquals(2, hits.size)
    }

    // ─── Radius Query Tests ──────────────────────────────────────────

    @Test
    fun `getAsteroidsInRadius finds asteroids within radius`() {
        val asteroids = listOf(
            createAsteroidAt(50f, 0f, radius = 20f),
            createAsteroidAt(200f, 0f, radius = 20f)
        )

        val found = collisionSystem.getAsteroidsInRadius(0f, 0f, 100f, asteroids)

        assertEquals(1, found.size)
        assertEquals(50f, found[0].position.x, 0.001f)
    }

    @Test
    fun `getAsteroidsInRadius accounts for asteroid radius`() {
        val asteroids = listOf(
            createAsteroidAt(120f, 0f, radius = 30f)  // Center at 120, edge at 90
        )

        val found = collisionSystem.getAsteroidsInRadius(0f, 0f, 100f, asteroids)

        assertEquals(1, found.size)  // 100 + 30 = 130 > 120
    }

    @Test
    fun `getAsteroidsInRadius excludes inactive asteroids`() {
        val asteroids = listOf(
            createAsteroidAt(50f, 0f).apply { isActive = false }
        )

        val found = collisionSystem.getAsteroidsInRadius(0f, 0f, 100f, asteroids)

        assertTrue(found.isEmpty())
    }

    @Test
    fun `getEnemiesInRadius finds enemies within radius`() {
        val enemies = listOf(
            createEnemyAt(50f, 0f, radius = 20f),
            createEnemyAt(200f, 0f, radius = 20f)
        )

        val found = collisionSystem.getEnemiesInRadius(0f, 0f, 100f, enemies)

        assertEquals(1, found.size)
    }

    @Test
    fun `getEnemiesInRadius uses squared distance for efficiency`() {
        // Just verify it works correctly with various positions
        val enemies = listOf(
            createEnemyAt(30f, 40f, radius = 10f)  // Distance 50 from origin
        )

        val found = collisionSystem.getEnemiesInRadius(0f, 0f, 55f, enemies)

        assertEquals(1, found.size)
    }

    // ─── Solar Storm LIGHTNING guard ────────────────────────────────

    @Test
    fun `LIGHTNING projectile with bounceCount=99 is not killed by collision`() {
        // Set up a LIGHTNING projectile overlapping an asteroid (bounceCount=99 marks it as
        // a Solar Storm visual that should skip collision)
        val asteroid = createAsteroidAt(0f, 0f, radius = 30f)

        val projectile = Projectile().apply {
            position.set(0f, 0f)
            type = ProjectileType.LIGHTNING
            bounceCount = 99
            radius = 10f
            isActive = true
            damage = 1f
        }

        val result = collisionSystem.checkCollisions(
            ship = Ship().apply { position.set(9999f, 9999f); radius = 5f },
            asteroids = listOf(asteroid),
            projectiles = listOf(projectile),
            powerUps = emptyList(),
            pickupRange = 0f,
            pullSpeed = 0f
        )

        assertTrue("LIGHTNING projectile with bounceCount=99 must remain active", projectile.isActive)
        assertTrue("No asteroid hits should be recorded for skipped projectile", result.asteroidHits.isEmpty())
    }

    // ─── Enemy-Asteroid Collision Tests ─────────────────────────────

    @Test
    fun `checkEnemyAsteroidCollisions enemy takes damage when overlapping asteroid`() {
        // Enemy and asteroid that clearly overlap
        val enemy = createEnemyAt(0f, 0f, radius = 20f).apply {
            health = 100f
            maxHealth = 100f
            spawnShieldTimer = 0f  // Disable spawn shield so takeDamage works
        }
        val asteroid = createAsteroidAt(10f, 0f, radius = 20f).apply {
            // distance between centers = 10, radii sum = 40 → collision
            damage = 25f
        }
        val asteroids = listOf(asteroid)

        // checkCollisions() must run first — it populates the spatial hash used by checkEnemyAsteroidCollisions
        val dummyShip = Ship().apply { position.set(99999f, 99999f); radius = 5f }
        collisionSystem.checkCollisions(dummyShip, asteroids, emptyList(), emptyList(), 0f, 0f)

        val destroyed = collisionSystem.checkEnemyAsteroidCollisions(
            enemies = listOf(enemy),
            asteroids = asteroids
        )

        // Enemy should have taken damage but not be destroyed (100 - 25 = 75)
        assertTrue("Damaged enemy should not be in destroyed list", destroyed.isEmpty())
        assertTrue("Enemy health should have decreased", enemy.health < 100f)
    }

    @Test
    fun `checkEnemyAsteroidCollisions lethal asteroid destroys enemy`() {
        // Enemy with barely any health, asteroid with high damage
        val enemy = createEnemyAt(0f, 0f, radius = 20f).apply {
            health = 1f
            maxHealth = 100f
            spawnShieldTimer = 0f  // Disable spawn shield so takeDamage works
        }
        val asteroid = createAsteroidAt(10f, 0f, radius = 20f).apply {
            // distance = 10, radii sum = 40 → collision
            damage = 50f
        }
        val asteroids = listOf(asteroid)

        // checkCollisions() must run first — it populates the spatial hash used by checkEnemyAsteroidCollisions
        val dummyShip = Ship().apply { position.set(99999f, 99999f); radius = 5f }
        collisionSystem.checkCollisions(dummyShip, asteroids, emptyList(), emptyList(), 0f, 0f)

        val destroyed = collisionSystem.checkEnemyAsteroidCollisions(
            enemies = listOf(enemy),
            asteroids = asteroids
        )

        assertEquals("Enemy with lethal hit should be in destroyed list", 1, destroyed.size)
        assertTrue("Destroyed enemy should be the one we created", destroyed[0] === enemy)
    }

    // ─── Helper Methods ─────────────────────────────────────────────

    private fun invokePointToLineSegmentDistance(
        px: Float, py: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float
    ): Float {
        // Use reflection to test private method
        val method = CollisionSystem::class.java.getDeclaredMethod(
            "pointToLineSegmentDistance",
            Float::class.java, Float::class.java,
            Float::class.java, Float::class.java,
            Float::class.java, Float::class.java
        )
        method.isAccessible = true
        return method.invoke(collisionSystem, px, py, x1, y1, x2, y2) as Float
    }

    private fun invokeCheckBeamCollision(beam: Projectile, target: Entity): Boolean {
        val method = CollisionSystem::class.java.getDeclaredMethod(
            "checkBeamCollision",
            Projectile::class.java, Entity::class.java
        )
        method.isAccessible = true
        return method.invoke(collisionSystem, beam, target) as Boolean
    }

    private fun createBeamProjectile(
        x: Float,
        y: Float,
        angle: Float,
        length: Float,
        halfWidth: Float
    ): Projectile {
        return Projectile().apply {
            position.set(x, y)
            type = ProjectileType.BEAM
            beamAngle = angle
            this.length = length
            radius = halfWidth  // Radius is used as half-width for beams
            isActive = true
        }
    }

    private fun createAsteroidAt(x: Float, y: Float, radius: Float = 20f): Asteroid {
        return Asteroid().apply {
            position.set(x, y)
            this.radius = radius
            isActive = true
        }
    }

    private fun createEnemyAt(x: Float, y: Float, radius: Float = 20f): EnemyShip {
        return EnemyShip().apply {
            position.set(x, y)
            this.radius = radius
            isActive = true
        }
    }
}

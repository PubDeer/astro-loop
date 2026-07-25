package com.astroloop.game.entity

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for Entity base class functionality.
 * Tests collision detection, distance calculations, and screen wrapping.
 */
class EntityTest {

    // Create a concrete implementation for testing
    class TestEntity : Entity() {
        fun setPosition(x: Float, y: Float) {
            position.set(x, y)
        }

        fun setVelocity(vx: Float, vy: Float) {
            velocity.set(vx, vy)
        }
    }

    // ─── Collision Detection ─────────────────────────────────────────

    @Test
    fun `collidesWith returns true for overlapping entities`() {
        val entity1 = TestEntity().apply {
            setPosition(0f, 0f)
            radius = 10f
        }

        val entity2 = TestEntity().apply {
            setPosition(15f, 0f)
            radius = 10f
        }

        // 10 + 10 = 20, distance = 15, should collide
        assertTrue(entity1.collidesWith(entity2))
    }

    @Test
    fun `collidesWith returns false for non-overlapping entities`() {
        val entity1 = TestEntity().apply {
            setPosition(0f, 0f)
            radius = 10f
        }

        val entity2 = TestEntity().apply {
            setPosition(25f, 0f)
            radius = 10f
        }

        // 10 + 10 = 20, distance = 25, should not collide
        assertFalse(entity1.collidesWith(entity2))
    }

    @Test
    fun `collidesWith returns true for touching entities`() {
        val entity1 = TestEntity().apply {
            setPosition(0f, 0f)
            radius = 10f
        }

        val entity2 = TestEntity().apply {
            setPosition(20f, 0f)
            radius = 10f
        }

        // 10 + 10 = 20, distance = 20, exactly touching
        assertTrue(entity1.collidesWith(entity2))
    }

    @Test
    fun `collidesWith works diagonally`() {
        val entity1 = TestEntity().apply {
            setPosition(0f, 0f)
            radius = 10f
        }

        val entity2 = TestEntity().apply {
            setPosition(10f, 10f)  // Distance = sqrt(200) ~ 14.14
            radius = 10f
        }

        // 10 + 10 = 20, distance ~ 14.14, should collide
        assertTrue(entity1.collidesWith(entity2))
    }

    @Test
    fun `collidesWith is symmetric`() {
        val entity1 = TestEntity().apply {
            setPosition(0f, 0f)
            radius = 10f
        }

        val entity2 = TestEntity().apply {
            setPosition(15f, 0f)
            radius = 10f
        }

        assertEquals(entity1.collidesWith(entity2), entity2.collidesWith(entity1))
    }

    // ─── Distance Calculations ───────────────────────────────────────

    @Test
    fun `distanceTo returns correct distance`() {
        val entity1 = TestEntity().apply { setPosition(0f, 0f) }
        val entity2 = TestEntity().apply { setPosition(3f, 4f) }

        assertEquals(5f, entity1.distanceTo(entity2), 0.001f)
    }

    @Test
    fun `distanceTo returns zero for same position`() {
        val entity1 = TestEntity().apply { setPosition(10f, 20f) }
        val entity2 = TestEntity().apply { setPosition(10f, 20f) }

        assertEquals(0f, entity1.distanceTo(entity2), 0.001f)
    }

    @Test
    fun `distanceToSquared returns squared distance`() {
        val entity1 = TestEntity().apply { setPosition(0f, 0f) }
        val entity2 = TestEntity().apply { setPosition(3f, 4f) }

        assertEquals(25f, entity1.distanceToSquared(entity2), 0.001f)
    }

    @Test
    fun `distanceToSquared is faster than distanceTo`() {
        // This is more of a documentation test - squared distance avoids sqrt
        val entity1 = TestEntity().apply { setPosition(0f, 0f) }
        val entity2 = TestEntity().apply { setPosition(100f, 100f) }

        val distSq = entity1.distanceToSquared(entity2)
        val dist = entity1.distanceTo(entity2)

        assertEquals(distSq, dist * dist, 0.001f)
    }

    // ─── Screen Wrapping ─────────────────────────────────────────────

    @Test
    fun `wrapAround wraps entity that exits right edge`() {
        val entity = TestEntity().apply {
            setPosition(1100f, 500f)
            radius = 20f
        }

        entity.wrapAround(1080f, 1920f)

        assertTrue(entity.position.x < 0f)
    }

    @Test
    fun `wrapAround wraps entity that exits left edge`() {
        val entity = TestEntity().apply {
            setPosition(-30f, 500f)
            radius = 20f
        }

        entity.wrapAround(1080f, 1920f)

        assertTrue(entity.position.x > 1080f)
    }

    @Test
    fun `wrapAround wraps entity that exits bottom edge`() {
        val entity = TestEntity().apply {
            setPosition(500f, 1950f)
            radius = 20f
        }

        entity.wrapAround(1080f, 1920f)

        assertTrue(entity.position.y < 0f)
    }

    @Test
    fun `wrapAround wraps entity that exits top edge`() {
        val entity = TestEntity().apply {
            setPosition(500f, -30f)
            radius = 20f
        }

        entity.wrapAround(1080f, 1920f)

        assertTrue(entity.position.y > 1920f)
    }

    @Test
    fun `wrapAround does nothing for entity within bounds`() {
        val entity = TestEntity().apply {
            setPosition(500f, 500f)
            radius = 20f
        }

        entity.wrapAround(1080f, 1920f)

        assertEquals(500f, entity.position.x, 0.001f)
        assertEquals(500f, entity.position.y, 0.001f)
    }

    // ─── Off Screen Detection ────────────────────────────────────────

    @Test
    fun `isOffScreen returns true when entity is off right`() {
        val entity = TestEntity().apply {
            setPosition(1200f, 500f)
            radius = 20f
        }

        assertTrue(entity.isOffScreen(1080f, 1920f))
    }

    @Test
    fun `isOffScreen returns true when entity is off left`() {
        val entity = TestEntity().apply {
            setPosition(-50f, 500f)
            radius = 20f
        }

        assertTrue(entity.isOffScreen(1080f, 1920f))
    }

    @Test
    fun `isOffScreen returns true when entity is off bottom`() {
        val entity = TestEntity().apply {
            setPosition(500f, 2000f)
            radius = 20f
        }

        assertTrue(entity.isOffScreen(1080f, 1920f))
    }

    @Test
    fun `isOffScreen returns true when entity is off top`() {
        val entity = TestEntity().apply {
            setPosition(500f, -50f)
            radius = 20f
        }

        assertTrue(entity.isOffScreen(1080f, 1920f))
    }

    @Test
    fun `isOffScreen returns false when entity is on screen`() {
        val entity = TestEntity().apply {
            setPosition(500f, 500f)
            radius = 20f
        }

        assertFalse(entity.isOffScreen(1080f, 1920f))
    }

    @Test
    fun `isOffScreen respects margin parameter`() {
        val entity = TestEntity().apply {
            setPosition(1150f, 500f)  // Off screen by 70
            radius = 20f
        }

        // With margin of 100, entity is within bounds
        assertFalse(entity.isOffScreen(1080f, 1920f, margin = 100f))

        // With margin of 50, entity is out of bounds
        assertTrue(entity.isOffScreen(1080f, 1920f, margin = 50f))
    }

    // ─── Damage System ───────────────────────────────────────────────

    @Test
    fun `takeDamage reduces health`() {
        val entity = TestEntity().apply {
            maxHealth = 100f
            health = 100f
        }

        entity.takeDamage(30f)

        assertEquals(70f, entity.health, 0.001f)
    }

    @Test
    fun `takeDamage returns true when health reaches zero`() {
        val entity = TestEntity().apply {
            maxHealth = 100f
            health = 50f
        }

        val destroyed = entity.takeDamage(50f)

        assertTrue(destroyed)
    }

    @Test
    fun `takeDamage returns false when health remains`() {
        val entity = TestEntity().apply {
            maxHealth = 100f
            health = 100f
        }

        val destroyed = entity.takeDamage(30f)

        assertFalse(destroyed)
    }

    @Test
    fun `takeDamage returns true for overkill damage`() {
        val entity = TestEntity().apply {
            maxHealth = 100f
            health = 50f
        }

        val destroyed = entity.takeDamage(200f)

        assertTrue(destroyed)
    }

    // ─── Update System ───────────────────────────────────────────────

    @Test
    fun `update moves entity based on velocity`() {
        val entity = TestEntity().apply {
            setPosition(100f, 100f)
            setVelocity(50f, -30f)
        }

        entity.update(1f)  // 1 second

        assertEquals(150f, entity.position.x, 0.001f)
        assertEquals(70f, entity.position.y, 0.001f)
    }

    @Test
    fun `update applies rotation speed`() {
        val entity = TestEntity().apply {
            rotation = 0f
            rotationSpeed = 1f  // 1 radian per second
        }

        entity.update(0.5f)  // 0.5 seconds

        assertEquals(0.5f, entity.rotation, 0.001f)
    }

    @Test
    fun `update scales with delta time`() {
        val entity = TestEntity().apply {
            setPosition(0f, 0f)
            setVelocity(100f, 100f)
        }

        entity.update(0.016f)  // ~60 FPS

        assertEquals(1.6f, entity.position.x, 0.001f)
        assertEquals(1.6f, entity.position.y, 0.001f)
    }

    // ─── Reset ───────────────────────────────────────────────────────

    @Test
    fun `reset restores default values`() {
        val entity = TestEntity().apply {
            setPosition(500f, 500f)
            setVelocity(100f, 100f)
            rotation = 1.5f
            rotationSpeed = 2f
            isActive = false
            health = 50f
            maxHealth = 100f
        }

        entity.reset()

        assertEquals(0f, entity.position.x, 0.001f)
        assertEquals(0f, entity.position.y, 0.001f)
        assertEquals(0f, entity.velocity.x, 0.001f)
        assertEquals(0f, entity.velocity.y, 0.001f)
        assertEquals(0f, entity.rotation, 0.001f)
        assertEquals(0f, entity.rotationSpeed, 0.001f)
        assertTrue(entity.isActive)
        assertEquals(entity.maxHealth, entity.health, 0.001f)
    }
}

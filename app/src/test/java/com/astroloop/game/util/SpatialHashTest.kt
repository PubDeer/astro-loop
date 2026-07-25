package com.astroloop.game.util

import com.astroloop.game.entity.Entity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SpatialHashTest {

    private open class TestEntity(x: Float, y: Float, r: Float = 10f) : Entity() {
        init {
            position.set(x, y)
            radius = r
            isActive = true
        }
    }

    private lateinit var hash: SpatialHash

    @Before
    fun setup() {
        hash = SpatialHash(100f)
    }

    @Test
    fun `query returns entity within radius`() {
        val e = TestEntity(50f, 50f)
        hash.insert(e)
        val results = hash.query(50f, 50f, 20f)
        assertTrue(results.contains(e))
    }

    @Test
    fun `query does not return entity outside radius`() {
        val e = TestEntity(500f, 500f)
        hash.insert(e)
        val results = hash.query(0f, 0f, 20f)
        assertFalse(results.contains(e))
    }

    @Test
    fun `entity spanning two cells is returned only once`() {
        // Entity at cell boundary: position 100, radius 60 spans adjacent cells
        val e = TestEntity(100f, 50f, 60f)
        hash.insert(e)
        val results = hash.query(100f, 50f, 80f)
        assertEquals(1, results.count { it === e })
    }

    @Test
    fun `query result has correct count for multiple entities`() {
        val e1 = TestEntity(10f, 10f)
        val e2 = TestEntity(20f, 20f)
        val e3 = TestEntity(5000f, 5000f)  // far away
        hash.insert(e1)
        hash.insert(e2)
        hash.insert(e3)
        val results = hash.query(15f, 15f, 50f)
        assertTrue(results.contains(e1))
        assertTrue(results.contains(e2))
        assertFalse(results.contains(e3))
    }
}

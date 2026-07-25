package com.astroloop.game.entity

import org.junit.Assert.*
import org.junit.Test

class EntityPoolReuseTest {

    private class TestEntity : Entity()

    @Test
    fun `getActiveEntities with result list populates only active entities`() {
        val pool = EntityPool({ TestEntity() }, 5)
        val a = pool.obtain()
        val b = pool.obtain()
        val c = pool.obtain()
        b.isActive = false  // simulate dead entity still in inUse

        val result = mutableListOf<TestEntity>()
        pool.getActiveEntities(result)

        assertEquals(2, result.size)
        assertTrue(result.contains(a))
        assertTrue(result.contains(c))
        assertFalse(result.contains(b))
    }

    @Test
    fun `getActiveEntities with result list clears previous contents`() {
        val pool = EntityPool({ TestEntity() }, 5)
        pool.obtain()

        val result = mutableListOf<TestEntity>()
        result.add(TestEntity())  // pre-populate with stale data

        pool.getActiveEntities(result)

        assertEquals(1, result.size)  // stale entry replaced
    }
}

package com.astroloop.game.util

import org.junit.Assert.assertEquals
import org.junit.Test

class Collision2DTest {
    // Box: x in [0,10], y in [0,10]

    @Test
    fun `no overlap returns center unchanged`() {
        val (x, y) = Collision2D.resolveCircleOutOfAabb(20f, 20f, 2f, 0f, 0f, 10f, 10f)
        assertEquals(20f, x, 0.001f)
        assertEquals(20f, y, 0.001f)
    }

    @Test
    fun `touching exactly at radius does not move`() {
        val (x, y) = Collision2D.resolveCircleOutOfAabb(12f, 5f, 2f, 0f, 0f, 10f, 10f)
        assertEquals(12f, x, 0.001f)
        assertEquals(5f, y, 0.001f)
    }

    @Test
    fun `overlap from the right pushes east to exactly radius`() {
        val (x, y) = Collision2D.resolveCircleOutOfAabb(11f, 5f, 2f, 0f, 0f, 10f, 10f)
        assertEquals(12f, x, 0.001f)
        assertEquals(5f, y, 0.001f)
    }

    @Test
    fun `overlap from above pushes north`() {
        val (x, y) = Collision2D.resolveCircleOutOfAabb(5f, -1f, 2f, 0f, 0f, 10f, 10f)
        assertEquals(5f, x, 0.001f)
        assertEquals(-2f, y, 0.001f)
    }

    @Test
    fun `center inside box ejects along shallowest face`() {
        val (x, y) = Collision2D.resolveCircleOutOfAabb(1f, 5f, 2f, 0f, 0f, 10f, 10f)
        assertEquals(-2f, x, 0.001f)
        assertEquals(5f, y, 0.001f)
    }
}

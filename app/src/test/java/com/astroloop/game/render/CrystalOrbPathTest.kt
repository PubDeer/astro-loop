package com.astroloop.game.render

import org.junit.Assert.*
import org.junit.Test

class CrystalOrbPathTest {

    @Test
    fun `starts exactly at source`() {
        val (x, y) = CrystalOrbPath.position(0f, 10f, 20f, 200f, 300f)
        assertEquals(10f, x, 0.01f)
        assertEquals(20f, y, 0.01f)
    }

    @Test
    fun `ends exactly at destination`() {
        val (x, y) = CrystalOrbPath.position(1f, 10f, 20f, 200f, 300f)
        assertEquals(200f, x, 0.01f)
        assertEquals(300f, y, 0.01f)
    }

    @Test
    fun `corkscrews off the straight line mid-flight`() {
        val (_, y) = CrystalOrbPath.position(0.5f, 0f, 0f, 100f, 0f)
        assertTrue("Mid-flight must deviate from the straight line", kotlin.math.abs(y) > 1f)
    }
}

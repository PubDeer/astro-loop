package com.astroloop.game.weapon

import com.astroloop.game.weapon.weapons.EnergySaw
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class EnergySawDiscTest {

    @Test
    fun `disc count is always 1`() {
        val w = EnergySaw()
        for (level in 1..5) {
            w.level = level
            assertEquals("L$level disc count", 1, w.getDiscCount())
        }
    }

    @Test
    fun `tick rate is flat baseCooldown at all levels`() {
        val w = EnergySaw()
        for (level in 1..5) {
            w.level = level
            assertEquals("L$level tick rate", w.baseCooldown, w.getTickRate(), 0.001f)
        }
    }

    @Test
    fun `getDiscPositions returns exactly 1 position at all levels`() {
        val w = EnergySaw()
        for (level in 1..5) {
            w.level = level
            val positions = w.getDiscPositions(0f, 0f, 0f)
            assertEquals("L$level position count", 1, positions.size)
        }
    }

    @Test
    fun `disc radius scales with level`() {
        assertEquals(20f, EnergySaw().also { it.level = 1 }.discRadius, 0.001f)
        assertEquals(28f, EnergySaw().also { it.level = 2 }.discRadius, 0.001f)
        assertEquals(36f, EnergySaw().also { it.level = 3 }.discRadius, 0.001f)
        assertEquals(45f, EnergySaw().also { it.level = 4 }.discRadius, 0.001f)
        assertEquals(55f, EnergySaw().also { it.level = 5 }.discRadius, 0.001f)
    }

    @Test
    fun `reach scales with level`() {
        assertEquals(80f, EnergySaw().also { it.level = 1 }.reach, 0.001f)
        assertEquals(90f, EnergySaw().also { it.level = 2 }.reach, 0.001f)
        assertEquals(100f, EnergySaw().also { it.level = 3 }.reach, 0.001f)
        assertEquals(112f, EnergySaw().also { it.level = 4 }.reach, 0.001f)
        assertEquals(125f, EnergySaw().also { it.level = 5 }.reach, 0.001f)
    }

    @Test
    fun `single disc sits directly in front of the ship`() {
        val w = EnergySaw()
        w.level = 1
        // Ship at origin pointing right (rotation=0).
        val positions = w.getDiscPositions(0f, 0f, 0f)
        val disc = positions[0]
        // Should be at (reach * cos(0), reach * sin(0)) = (reach, 0)
        assertEquals(w.reach, disc.first, 0.001f)
        assertEquals(0f, disc.second, 0.001f)
    }

    @Test
    fun `disc position rotates with ship`() {
        val w = EnergySaw()
        w.level = 2
        val tau = 2f * 3.14159265f
        val quarterTurn = tau / 4f  // 90 degrees
        val positions = w.getDiscPositions(0f, 0f, quarterTurn)
        val disc = positions[0]
        // Should be at (reach * cos(π/2), reach * sin(π/2)) ≈ (0, reach)
        assertEquals(0f, disc.first, 0.001f)
        assertEquals(w.reach, disc.second, 0.001f)
    }
}

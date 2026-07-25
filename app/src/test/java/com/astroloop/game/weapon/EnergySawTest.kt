package com.astroloop.game.weapon

import com.astroloop.game.core.GameState
import com.astroloop.game.weapon.weapons.EnergySaw
import org.junit.Assert.assertEquals
import org.junit.Test

class EnergySawTest {

    private fun sawAt(level: Int) = EnergySaw().also { it.level = level }

    @Test
    fun `exactly one disc at every level`() {
        for (level in 1..5) assertEquals(1, sawAt(level).getDiscCount())
    }

    @Test
    fun `disc radius grows per level`() {
        assertEquals(20f, sawAt(1).discRadius, 0.001f)
        assertEquals(28f, sawAt(2).discRadius, 0.001f)
        assertEquals(36f, sawAt(3).discRadius, 0.001f)
        assertEquals(45f, sawAt(4).discRadius, 0.001f)
        assertEquals(55f, sawAt(5).discRadius, 0.001f)
    }

    @Test
    fun `reach grows per level`() {
        assertEquals(80f, sawAt(1).reach, 0.001f)
        assertEquals(90f, sawAt(2).reach, 0.001f)
        assertEquals(100f, sawAt(3).reach, 0.001f)
        assertEquals(112f, sawAt(4).reach, 0.001f)
        assertEquals(125f, sawAt(5).reach, 0.001f)
    }

    @Test
    fun `damage ladder keeps asteroid TTK roughly flat`() {
        val state = GameState()
        state.reset()
        assertEquals(8f, sawAt(1).getDamage(state), 0.001f)
        assertEquals(9f, sawAt(2).getDamage(state), 0.001f)
        assertEquals(10f, sawAt(3).getDamage(state), 0.001f)
        assertEquals(11f, sawAt(4).getDamage(state), 0.001f)
        assertEquals(12f, sawAt(5).getDamage(state), 0.001f)
    }

    @Test
    fun `single disc sits directly in front of the ship`() {
        val saw = sawAt(1)
        val positions = saw.getDiscPositions(0f, 0f, 0f)
        assertEquals(1, positions.size)
        assertEquals(80f, positions[0].first, 0.001f)
        assertEquals(0f, positions[0].second, 0.001f)
    }
}

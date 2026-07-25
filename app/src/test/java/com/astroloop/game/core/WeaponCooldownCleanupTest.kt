package com.astroloop.game.core

import org.junit.Assert.*
import org.junit.Test

class WeaponCooldownCleanupTest {

    /** Simulates the iterator-based cooldown cleanup logic. */
    private fun tickCooldowns(cooldowns: MutableMap<String, Float>, deltaTime: Float) {
        val iter = cooldowns.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            val newVal = entry.value - deltaTime
            if (newVal <= 0f) iter.remove() else entry.setValue(newVal)
        }
    }

    @Test
    fun `expired cooldowns are removed`() {
        val map = mutableMapOf("a" to 0.1f, "b" to 0.5f)
        tickCooldowns(map, 0.2f)
        assertFalse(map.containsKey("a"))
        assertTrue(map.containsKey("b"))
    }

    @Test
    fun `active cooldowns are decremented`() {
        val map = mutableMapOf("x" to 1.0f)
        tickCooldowns(map, 0.3f)
        assertEquals(0.7f, map["x"]!!, 0.001f)
    }

    @Test
    fun `empty map does not throw`() {
        val map = mutableMapOf<String, Float>()
        tickCooldowns(map, 0.016f)
        assertTrue(map.isEmpty())
    }
}

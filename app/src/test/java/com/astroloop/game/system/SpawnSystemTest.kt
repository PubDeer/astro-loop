package com.astroloop.game.system

import org.junit.Assert.*
import org.junit.Test

class SpawnSystemTest {

    @Test
    fun `asteroid count tracks the multiplier capped at 3`() {
        assertEquals(1, SpawnSystem.asteroidCount(1.0f))
        assertEquals(2, SpawnSystem.asteroidCount(2.25f)) // rounds to 2
        assertEquals(3, SpawnSystem.asteroidCount(2.75f)) // rounds to 3
        assertEquals(3, SpawnSystem.asteroidCount(4.0f))  // coerced to 3
    }

    @Test
    fun `asteroid count never drops below 1`() {
        assertEquals(1, SpawnSystem.asteroidCount(0.4f))
        assertEquals(1, SpawnSystem.asteroidCount(0f))
    }

    @Test
    fun `speed factor ramps with time and multiplier`() {
        // 4 min, mult 3.16: (1 + 4*0.15) * 3.16 = 5.056, below the cap
        assertEquals(5.056f, SpawnSystem.asteroidSpeedFactor(240f, 3.16f), 0.01f)
    }

    @Test
    fun `speed factor is capped at 7`() {
        // 8 min, mult 4.0: (1 + 8*0.15) * 4.0 = 8.8 -> clamped to 7.0
        assertEquals(7.0f, SpawnSystem.asteroidSpeedFactor(480f, 4.0f), 0.001f)
        // 10 min, mult 4.0: even higher -> still 7.0
        assertEquals(7.0f, SpawnSystem.asteroidSpeedFactor(600f, 4.0f), 0.001f)
    }
}

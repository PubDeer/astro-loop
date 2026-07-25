package com.astroloop.game.weapon

import com.astroloop.game.weapon.weapons.Railgun
import org.junit.Assert.assertEquals
import org.junit.Test

class RailgunTest {

    private fun railgunAt(level: Int) = Railgun().also { it.level = level }

    @Test
    fun `pierce count is flat 10 at every level`() {
        for (level in 1..5) assertEquals(10, railgunAt(level).getPierceCount())
    }

    @Test
    fun `shot collision radius widens per level`() {
        assertEquals(4f, railgunAt(1).getShotRadius(), 0.001f)
        assertEquals(5.5f, railgunAt(2).getShotRadius(), 0.001f)
        assertEquals(7f, railgunAt(3).getShotRadius(), 0.001f)
        assertEquals(8.5f, railgunAt(4).getShotRadius(), 0.001f)
        assertEquals(10f, railgunAt(5).getShotRadius(), 0.001f)
    }

    @Test
    fun `shot visual width widens per level`() {
        assertEquals(3f, railgunAt(1).getShotWidth(), 0.001f)
        assertEquals(5f, railgunAt(2).getShotWidth(), 0.001f)
        assertEquals(7f, railgunAt(3).getShotWidth(), 0.001f)
        assertEquals(9f, railgunAt(4).getShotWidth(), 0.001f)
        assertEquals(11f, railgunAt(5).getShotWidth(), 0.001f)
    }
}

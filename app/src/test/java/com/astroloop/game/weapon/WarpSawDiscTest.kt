package com.astroloop.game.weapon

import com.astroloop.game.weapon.weapons.WarpSaw
import org.junit.Assert.assertEquals
import org.junit.Test

class WarpSawDiscTest {

    @Test
    fun `getDiscCount returns 1`() {
        assertEquals(1, WarpSaw().getDiscCount())
    }

    @Test
    fun `idle getDiscPositions returns a single position`() {
        val positions = WarpSaw().getDiscPositions(0f, 0f, 0f)
        assertEquals(1, positions.size)
    }
}

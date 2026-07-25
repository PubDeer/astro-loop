package com.astroloop.game.weapon

import com.astroloop.game.weapon.weapons.ClusterBomb
import org.junit.Assert.assertEquals
import org.junit.Test

class ClusterBombTest {

    private fun bombAt(level: Int) = ClusterBomb().also { it.level = level }

    @Test
    fun `bomblet count is level plus one`() {
        assertEquals(2, bombAt(1).getBombletCount())
        assertEquals(3, bombAt(2).getBombletCount())
        assertEquals(4, bombAt(3).getBombletCount())
        assertEquals(5, bombAt(4).getBombletCount())
        assertEquals(6, bombAt(5).getBombletCount())
    }
}

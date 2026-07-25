package com.astroloop.game.system

import org.junit.Assert.assertEquals
import org.junit.Test

class FleetSystemFireTest {

    @Test
    fun `railgun ship maps to outer ring`() {
        assertEquals(0, FleetSystem.ringForShip("ship_white"))
    }

    @Test
    fun `energy saw ship maps to inner ring`() {
        assertEquals(1, FleetSystem.ringForShip("ship_magenta"))
    }

    @Test
    fun `unknown ship defaults to outer ring`() {
        assertEquals(0, FleetSystem.ringForShip("ship_does_not_exist"))
    }
}

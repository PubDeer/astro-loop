package com.astroloop.game.system

import com.astroloop.game.data.ShipDefinitions
import com.astroloop.game.weapon.WeaponFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CrewmateEncounterWeaponTest {
    @Test
    fun `ship_blue starting weapon is pulse_cannon`() {
        val shipDef = ShipDefinitions.getShip("ship_blue")
        assertEquals("pulse_cannon", shipDef?.startingWeaponId)
    }

    @Test
    fun `all base weapon IDs produce valid weapons`() {
        for (id in WeaponFactory.getBaseWeaponIds()) {
            assertNotNull("WeaponFactory returned null for $id", WeaponFactory.createWeapon(id))
        }
    }
}

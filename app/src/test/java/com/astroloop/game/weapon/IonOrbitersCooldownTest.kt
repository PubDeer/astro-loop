package com.astroloop.game.weapon

import com.astroloop.game.weapon.weapons.IonOrbiters
import org.junit.Assert.assertEquals
import org.junit.Test

class IonOrbitersCooldownTest {

    @Test
    fun `IonOrbiters baseCooldown is 4_0f`() {
        val weapon = IonOrbiters()
        assertEquals(4.0f, weapon.baseCooldown, 0.001f)
    }
}

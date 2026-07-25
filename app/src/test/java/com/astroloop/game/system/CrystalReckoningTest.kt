package com.astroloop.game.system

import org.junit.Assert.*
import org.junit.Test

class CrystalReckoningTest {
    @Test fun entersWhenAllConditionsHold() {
        assertTrue(CrystalReckoning.shouldEnter(true, "pilot_astro", 12, false))
        assertTrue(CrystalReckoning.shouldEnter(true, "pilot_astro", 13, false))
    }
    @Test fun blockedByEachCondition() {
        assertFalse(CrystalReckoning.shouldEnter(false, "pilot_astro", 12, false)) // not astro loop
        assertFalse(CrystalReckoning.shouldEnter(true, "pilot_medic", 12, false))  // wrong pilot
        assertFalse(CrystalReckoning.shouldEnter(true, "pilot_astro", 11, false))  // not enough bandanas
        assertFalse(CrystalReckoning.shouldEnter(true, "pilot_astro", 12, true))   // already released
    }
}

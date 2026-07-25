package com.astroloop.game.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ReckoningJustLostPersistenceTest {

    private fun newPm(): PersistenceManager {
        val pm = PersistenceManager(ApplicationProvider.getApplicationContext())
        pm.resetAllProgress()
        return pm
    }

    @Test fun defaultsFalse() {
        assertFalse(newPm().isReckoningJustLost())
    }

    @Test fun roundTrips() {
        val pm = newPm()
        pm.setReckoningJustLost(true)
        assertTrue(pm.isReckoningJustLost())
    }

    @Test fun resetClearsIt() {
        val pm = newPm()
        pm.setReckoningJustLost(true)
        pm.resetAllProgress()
        assertFalse(pm.isReckoningJustLost())
    }

    @Test fun winAndLossAreIndependentFlags() {
        val pm = newPm()
        pm.setReckoningJustLost(true)
        assertFalse("losing must never set the win flag", pm.isReckoningJustWon())
    }
}

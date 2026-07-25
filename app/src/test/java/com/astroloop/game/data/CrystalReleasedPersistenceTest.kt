package com.astroloop.game.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private fun makeTestPersistenceManager(): PersistenceManager {
    val pm = PersistenceManager(ApplicationProvider.getApplicationContext())
    pm.resetAllProgress()
    return pm
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CrystalReleasedPersistenceTest {

    private fun newPm(): PersistenceManager = makeTestPersistenceManager()

    @Test fun defaultsFalse() {
        assertFalse(newPm().isCrystalReleased())
    }

    @Test fun roundTrips() {
        val pm = newPm()
        pm.setCrystalReleased(true)
        assertTrue(pm.isCrystalReleased())
    }

    @Test fun resetClearsIt() {
        val pm = newPm()
        pm.setCrystalReleased(true)
        pm.resetAllProgress()
        assertFalse(pm.isCrystalReleased())
    }
}

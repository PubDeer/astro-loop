package com.astroloop.game.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ReckoningAttemptedPersistenceTest {

    private fun newPm(): PersistenceManager {
        val pm = PersistenceManager(ApplicationProvider.getApplicationContext())
        pm.resetAllProgress()
        return pm
    }

    @Test fun defaultsFalse() {
        assertFalse(newPm().isReckoningAttempted())
    }

    @Test fun roundTrips() {
        val pm = newPm()
        pm.setReckoningAttempted(true)
        assertTrue(pm.isReckoningAttempted())
    }

    @Test fun resetClearsIt() {
        val pm = newPm()
        pm.setReckoningAttempted(true)
        pm.resetAllProgress()
        assertFalse(pm.isReckoningAttempted())
    }
}

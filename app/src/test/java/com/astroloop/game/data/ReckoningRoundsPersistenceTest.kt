package com.astroloop.game.data

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ReckoningRoundsPersistenceTest {

    private fun pm() = PersistenceManager(RuntimeEnvironment.getApplication())

    @Test
    fun roundsStartAtZeroAndIncrement() {
        val p = pm()
        assertEquals(0, p.getReckoningRounds())
        p.incrementReckoningRounds()
        p.incrementReckoningRounds()
        assertEquals(2, p.getReckoningRounds())
    }

    @Test
    fun poolLastDefaultsToMinusOneAndRoundtrips() {
        val p = pm()
        assertEquals(-1, p.getReckoningPoolLast())
        p.setReckoningPoolLast(2)
        assertEquals(2, p.getReckoningPoolLast())
    }

    @Test
    fun setReckoningRoundsNeverGoesNegative() {
        val p = pm()
        p.setReckoningRounds(-5)
        assertEquals(0, p.getReckoningRounds())
    }

    @Test
    fun resetAllProgressClearsRoundState() {
        val p = pm()
        p.incrementReckoningRounds()
        p.setReckoningPoolLast(3)
        p.resetAllProgress()
        assertEquals(0, p.getReckoningRounds())
        assertEquals(-1, p.getReckoningPoolLast())
    }
}

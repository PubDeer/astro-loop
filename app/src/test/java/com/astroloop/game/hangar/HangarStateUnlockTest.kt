package com.astroloop.game.hangar

import androidx.test.core.app.ApplicationProvider
import com.astroloop.game.data.PersistenceManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HangarStateUnlockTest {

    private lateinit var persistence: PersistenceManager
    private lateinit var state: HangarState

    @Before
    fun setup() {
        persistence = PersistenceManager(ApplicationProvider.getApplicationContext())
        persistence.resetAllProgress()
        state = HangarState(persistence)
    }

    private fun setRunsSinceUnlock(n: Int) {
        repeat(n) { persistence.incrementRunsSincePilotUnlock() }
    }

    private fun unlockAllPilotsExceptAstro() {
        val pilotIds = listOf(
            "pilot_medic", "pilot_rascal", "pilot_brutus", "pilot_frost",
            "pilot_dash", "pilot_ember", "pilot_fang", "pilot_kraken",
            "pilot_whiskers", "pilot_unit7", "pilot_havoc"
        )
        pilotIds.forEach { persistence.unlockPilot(it) }
        persistence.setNextPilotIndex(11)  // Astro is next
    }

    private fun unlockAllShipsExcept(skipId: String) {
        val shipIds = listOf(
            "ship_blue", "ship_green", "ship_orange", "ship_cyan",
            "ship_lime", "ship_yellow", "ship_red", "ship_magenta",
            "ship_coral", "ship_indigo", "ship_purple"
        )
        shipIds.filter { it != skipId }.forEach { persistence.unlockShip(it) }
    }

    @Test
    fun `Astro does not unlock via 17-run fallback when Specter is locked`() {
        unlockAllPilotsExceptAstro()
        unlockAllShipsExcept("ship_white")  // Specter locked
        setRunsSinceUnlock(17)

        assertFalse(
            "Astro must not auto-unlock when Specter is not purchased",
            state.checkPilotUnlockCondition()
        )
    }

    @Test
    fun `Whiskers does not unlock via 17-run fallback`() {
        // Unlock all pilots up to Whiskers (index 8), set Whiskers as next
        val pilotIds = listOf(
            "pilot_medic", "pilot_rascal", "pilot_brutus", "pilot_frost",
            "pilot_dash", "pilot_ember", "pilot_fang", "pilot_kraken"
        )
        pilotIds.forEach { persistence.unlockPilot(it) }
        persistence.setNextPilotIndex(8)  // Whiskers is next
        setRunsSinceUnlock(17)

        assertFalse(
            "Whiskers must not auto-unlock — jackpot only",
            state.checkPilotUnlockCondition()
        )
    }

    @Test
    fun `Astro unlocks when all pilots and all ships are owned`() {
        unlockAllPilotsExceptAstro()
        unlockAllShipsExcept("ship_white")
        persistence.unlockShip("ship_white")  // Specter purchased
        setRunsSinceUnlock(2)

        assertTrue(
            "Astro must unlock once all pilots and ships are owned",
            state.checkPilotUnlockCondition()
        )
    }

    @Test
    fun `Astro is not fast-tracked post-desert when ships or pilots missing`() {
        unlockAllPilotsExceptAstro()
        unlockAllShipsExcept("ship_white")  // Specter still locked
        setRunsSinceUnlock(2)
        persistence.setStoryLoop(2)
        assertFalse(
            "Post-desert Astro must still require all ships",
            state.checkPilotUnlockCondition()
        )
    }

    @Test
    fun `Astro unlocks post-desert when all pilots ships and 2 runs met`() {
        unlockAllPilotsExceptAstro()
        unlockAllShipsExcept("ship_white")
        persistence.unlockShip("ship_white")
        setRunsSinceUnlock(2)
        persistence.setStoryLoop(2)
        assertTrue(
            "Post-desert Astro unlocks once all pilots+ships owned and 2 runs elapsed",
            state.checkPilotUnlockCondition()
        )
    }

    @Test
    fun `Other pilots still unlock with 1 run post-desert`() {
        persistence.unlockPilot("pilot_medic")
        persistence.setNextPilotIndex(1)
        setRunsSinceUnlock(1)
        persistence.setStoryLoop(2)
        assertTrue(
            "Non-Astro pilots unlock after 1 run post-desert",
            state.checkPilotUnlockCondition()
        )
    }
}

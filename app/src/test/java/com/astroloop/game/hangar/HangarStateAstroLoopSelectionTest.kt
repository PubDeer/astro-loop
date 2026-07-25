package com.astroloop.game.hangar

import androidx.test.core.app.ApplicationProvider
import com.astroloop.game.core.StoryStage
import com.astroloop.game.data.PersistenceManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HangarStateAstroLoopSelectionTest {

    private lateinit var persistence: PersistenceManager
    private lateinit var state: HangarState

    @Before
    fun setup() {
        persistence = PersistenceManager(ApplicationProvider.getApplicationContext())
        persistence.resetAllProgress()
        state = HangarState(persistence)
        state.pilotScreenWidth = 1000f
    }

    @Test
    fun `first entry defaults to Astro and Specter`() {
        persistence.setStoryStageCode(StoryStage.ASTRO_LOOP.code)
        persistence.setAstroLoopFirstEntry()

        state.initialize()

        assertEquals("pilot_astro", state.getSelectedPilot()?.id)
        assertEquals("ship_white", state.getSelectedShip()?.id)
    }

    @Test
    fun `later entry restores last-flown selection`() {
        persistence.setStoryStageCode(StoryStage.ASTRO_LOOP.code)
        // not first entry — flag intentionally left unset
        persistence.setSelectedPilotId("pilot_dash")
        persistence.setSelectedShipId("ship_green")

        state.initialize()

        assertEquals("pilot_dash", state.getSelectedPilot()?.id)
        assertEquals("ship_green", state.getSelectedShip()?.id)
    }
}

package com.astroloop.game.hangar

import androidx.test.core.app.ApplicationProvider
import com.astroloop.game.core.StoryStage
import com.astroloop.game.data.PersistenceManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The bare "Welcome back." used to fire only on the same-session return after the
 * timeline shift (resetForReturn with fadeFromWhite). A fresh app boot into Astro
 * Loop first entry must queue it too, and consume the first-entry flag so it fires
 * exactly once.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HangarStateAstroLoopBootWelcomeTest {

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
    fun `fresh boot on first entry queues TB welcome and consumes the flag`() {
        persistence.setStoryStageCode(StoryStage.ASTRO_LOOP.code)
        persistence.setAstroLoopFirstEntry()

        state.initialize()

        assertTrue("welcome must be queued on the boot path", state.pendingTbWelcome)
        assertFalse("first-entry flag must be consumed", persistence.isAstroLoopFirstEntry())
    }

    @Test
    fun `later boots do not queue the welcome`() {
        persistence.setStoryStageCode(StoryStage.ASTRO_LOOP.code)

        state.initialize()

        assertFalse(state.pendingTbWelcome)
    }

    @Test
    fun `non astro loop boots do not queue the welcome`() {
        state.initialize()

        assertFalse(state.pendingTbWelcome)
    }
}

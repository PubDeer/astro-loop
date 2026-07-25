// app/src/test/java/com/astroloop/game/hangar/ChatSystemAstroHintTest.kt
package com.astroloop.game.hangar

import androidx.test.core.app.ApplicationProvider
import com.astroloop.game.data.PersistenceManager
import com.astroloop.game.data.PilotDefinitions
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ChatSystemAstroHintTest {

    private lateinit var persistence: PersistenceManager
    private lateinit var state: HangarState
    private lateinit var chatSystem: ChatSystem

    @Before
    fun setup() {
        persistence = PersistenceManager(ApplicationProvider.getApplicationContext())
        persistence.resetAllProgress()
        state = HangarState(persistence)
        state.currentPage = 0  // hints only fire on the bar page
        chatSystem = ChatSystem()
    }

    private fun unlockNonAstroCrew() {
        PilotDefinitions.pilots.dropLast(1).forEach { persistence.unlockPilot(it.id) }
    }

    @Test
    fun `astro hints can fire while astro is still locked`() {
        unlockNonAstroCrew()

        repeat(300) { chatSystem.addIdleLine(state) }

        assertTrue("expected at least one astro hint before Astro is recruited",
            state.astroHintCount > 0)
    }

    @Test
    fun `astro hints never fire once astro is recruited`() {
        unlockNonAstroCrew()
        persistence.unlockPilot("pilot_astro")

        repeat(300) { chatSystem.addIdleLine(state) }

        assertEquals("Astro is at the bar — TB-26 must stop hinting about him",
            0, state.astroHintCount)
    }

    @Test
    fun `recruiting astro retires the persisted hint state`() {
        unlockNonAstroCrew()
        persistence.unlockPilot("pilot_astro")
        assertFalse(persistence.isAstroHinted())

        chatSystem.addIdleLine(state)

        assertTrue("saves where Astro arrived before the third hint must self-heal",
            persistence.isAstroHinted())
    }
}

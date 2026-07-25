// app/src/test/java/com/astroloop/game/hangar/HangarStateWalkerRebuildTest.kt
package com.astroloop.game.hangar

import androidx.test.core.app.ApplicationProvider
import com.astroloop.game.core.StoryStage
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
class HangarStateWalkerRebuildTest {

    private lateinit var persistence: PersistenceManager
    private lateinit var state: HangarState

    private val astroIndex = PilotDefinitions.getPilotCount() - 1

    @Before
    fun setup() {
        persistence = PersistenceManager(ApplicationProvider.getApplicationContext())
        persistence.resetAllProgress()
        state = HangarState(persistence)
    }

    private fun unlockEveryone() {
        PilotDefinitions.pilots.forEach { persistence.unlockPilot(it.id) }
    }

    @Test
    fun `rebuild populates walkers for all unlocked pilots except the selected one`() {
        unlockEveryone()
        persistence.setStoryStageCode(StoryStage.ASTRO_LOOP.code)
        state.selectedPilotIndex = astroIndex
        assertTrue("astro-loop entry starts from a stale-empty list", state.npcWalkers.isEmpty())

        state.rebuildNpcWalkers()

        assertEquals(PilotDefinitions.getPilotCount() - 1, state.npcWalkers.size)
        assertTrue("selected pilot never walks the bar",
            state.npcWalkers.none { it.pilotIndex == astroIndex })
    }

    @Test
    fun `rebuild in corruption filters dead pilots and astro`() {
        unlockEveryone()
        persistence.setStoryStageCode(StoryStage.CORRUPTION.code)
        persistence.addDeadPilot("pilot_havoc")
        persistence.addDeadPilot("pilot_unit7")
        state.selectedPilotIndex = 0  // Medic

        state.rebuildNpcWalkers()

        // 12 minus selected Medic, Astro (slot machine), and the 2 dead = 8
        assertEquals(8, state.npcWalkers.size)
        val indexes = state.npcWalkers.map { it.pilotIndex }
        assertFalse(indexes.contains(astroIndex))
        assertFalse(indexes.contains(9))   // Unit-7
        assertFalse(indexes.contains(10))  // Havoc
    }

    @Test
    fun `rebuild clears stale pending walker mutations`() {
        unlockEveryone()
        state.selectedPilotIndex = 0
        state.pendingNPCAdds.add(WalkerNPC(pilotIndex = 3, color = 0,
            x = 0.5f, targetX = 0.5f, walking = false, idleTimer = 1f))
        state.pendingNPCRemoves.add(4)

        state.rebuildNpcWalkers()

        assertTrue(state.pendingNPCAdds.isEmpty())
        assertTrue(state.pendingNPCRemoves.isEmpty())
    }
}

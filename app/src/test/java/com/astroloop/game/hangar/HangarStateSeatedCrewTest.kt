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
class HangarStateSeatedCrewTest {

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
    fun `lowest free stool prefers the seatable set in order`() {
        assertEquals(2, state.lowestFreeStool(emptySet()))
        assertEquals(3, state.lowestFreeStool(setOf(2)))
        assertEquals(5, state.lowestFreeStool(setOf(2, 3)))
        assertEquals(-1, state.lowestFreeStool(setOf(2, 3, 5, 7)))
    }

    @Test
    fun `stool normalized x maps into the walker 0-1 band`() {
        // stool 2 screen x = 10 + (1000-20)/9*2 = 227.78; normalized = (227.78-100)/800
        assertEquals(0.1597f, state.stoolNormalizedX(2), 0.001f)
        assertTrue(state.stoolNormalizedX(7) in 0f..1f)
    }

    @Test
    fun `astro loop walker heads for a stool when choosing a destination`() {
        persistence.setStoryStageCode(StoryStage.ASTRO_LOOP.code)
        val npc = WalkerNPC(pilotIndex = 0, color = 0xFFFFFFFF.toInt(),
            x = 0.5f, targetX = 0.5f, walking = false, idleTimer = 0f)
        state.npcWalkers.add(npc)

        state.updateNPCWalkers(0.016f)

        assertTrue("walker should be en route", npc.walking)
        assertEquals("targets the lowest free stool", 2, npc.pendingStool)
        assertEquals(state.stoolNormalizedX(2), npc.targetX, 0.001f)
    }

    @Test
    fun `walker sits on arrival at its pending stool`() {
        persistence.setStoryStageCode(StoryStage.ASTRO_LOOP.code)
        val target = state.stoolNormalizedX(2)
        val npc = WalkerNPC(pilotIndex = 0, color = 0xFFFFFFFF.toInt(),
            x = target, targetX = target, walking = true, idleTimer = 5f)
        npc.pendingStool = 2
        state.npcWalkers.add(npc)

        state.updateNPCWalkers(0.016f)

        assertTrue("walker seated", npc.seated)
        assertEquals(2, npc.seatedStool)
        assertEquals(-1, npc.pendingStool)
        assertFalse(npc.walking)
    }

    @Test
    fun `normal stage walker heads for a stool when choosing a destination`() {
        assertEquals(StoryStage.NORMAL, com.astroloop.game.core.StoryStateManager.stage(persistence))
        val npc = WalkerNPC(pilotIndex = 0, color = 0xFFFFFFFF.toInt(),
            x = 0.5f, targetX = 0.5f, walking = false, idleTimer = 0f)
        state.npcWalkers.add(npc)

        state.updateNPCWalkers(0.016f)

        assertTrue("walker should be en route", npc.walking)
        assertEquals("targets the lowest free stool", 2, npc.pendingStool)
    }

    @Test
    fun `corruption stage never assigns a stool`() {
        persistence.setStoryStageCode(StoryStage.CORRUPTION.code)
        val npc = WalkerNPC(pilotIndex = 0, color = 0xFFFFFFFF.toInt(),
            x = 0.5f, targetX = 0.5f, walking = false, idleTimer = 0f)
        state.npcWalkers.add(npc)

        state.updateNPCWalkers(0.016f)

        assertEquals(-1, npc.pendingStool)
        assertFalse(npc.seated)
    }

    @Test
    fun `corruption clears stale seated state`() {
        persistence.setStoryStageCode(StoryStage.CORRUPTION.code)
        val npc = WalkerNPC(pilotIndex = 0, color = 0xFFFFFFFF.toInt(),
            x = 0.5f, targetX = 0.5f, walking = false, idleTimer = 5f)
        npc.seated = true
        npc.seatedStool = 2
        state.npcWalkers.add(npc)

        state.updateNPCWalkers(0.016f)

        assertFalse("nobody lounges at the bar in corruption", npc.seated)
        assertEquals(-1, npc.seatedStool)
    }
}

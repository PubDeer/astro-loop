package com.astroloop.game.hangar

import androidx.test.core.app.ApplicationProvider
import com.astroloop.game.core.StoryStage
import com.astroloop.game.core.StoryStateManager
import com.astroloop.game.data.PersistenceManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HangarStateWalkSpeedTest {

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
    fun `pilot walks at full speed in normal mode`() {
        assertEquals(StoryStage.NORMAL, StoryStateManager.stage(persistence))

        state.pilotX = 0f
        state.pilotTargetX = 1000f
        state.pilotWalking = true

        state.updatePilotWalker(1.0f)

        // Full speed = 0.70 * 1000 = 700px/s
        assertEquals("Normal mode walk speed must be full (700px/s)", 700f, state.pilotX, 1f)
    }

    @Test
    fun `pilot walks at full speed in astro loop stage`() {
        persistence.setStoryStageCode(StoryStage.ASTRO_LOOP.code)
        assertTrue("astro loop stage must be active to make the test meaningful",
            StoryStateManager.isAstroLoop(persistence))
        assertFalse("astro loop is not corruption", StoryStateManager.isCorrupted(persistence))

        state.pilotX = 0f
        state.pilotTargetX = 1000f
        state.pilotWalking = true

        state.updatePilotWalker(1.0f)

        // Must be full speed (700px/s), not corruption half-speed (350px/s)
        assertEquals("Astro Loop walk speed must be full (700px/s)", 700f, state.pilotX, 1f)
    }

    @Test
    fun `pilot walks at half speed in corruption mode`() {
        persistence.setStoryStageCode(StoryStage.CORRUPTION.code)
        assertFalse("Astro Loop must be off for this test", StoryStateManager.isAstroLoop(persistence))

        state.pilotX = 0f
        state.pilotTargetX = 1000f
        state.pilotWalking = true

        state.updatePilotWalker(1.0f)

        // Corruption half-speed = 0.70 * 0.5 * 1000 = 350px/s
        assertEquals("Corruption walk speed must be halved (350px/s)", 350f, state.pilotX, 1f)
    }

    @Test
    fun `npc walks at full speed in normal mode`() {
        assertEquals(StoryStage.NORMAL, StoryStateManager.stage(persistence))

        val npc = WalkerNPC(pilotIndex = 0, color = 0xFF_FFFFFF.toInt(), x = 0f, targetX = 1f, walking = true, idleTimer = 0f)
        state.npcWalkers.add(npc)

        state.updateNPCWalkers(1.0f)

        // Full speed = 0.105f per second
        assertEquals("Normal mode NPC walk speed must be full", 0.105f, npc.x, 0.001f)
    }

    @Test
    fun `npc walks at full speed in astro loop stage`() {
        persistence.setStoryStageCode(StoryStage.ASTRO_LOOP.code)
        assertTrue("astro loop stage must be active to make the test meaningful",
            StoryStateManager.isAstroLoop(persistence))
        assertFalse("astro loop is not corruption", StoryStateManager.isCorrupted(persistence))

        val npc = WalkerNPC(pilotIndex = 0, color = 0xFF_FFFFFF.toInt(), x = 0f, targetX = 1f, walking = true, idleTimer = 0f)
        state.npcWalkers.add(npc)

        state.updateNPCWalkers(1.0f)

        // Must be full speed (0.105f), not corruption half-speed (0.0525f)
        assertEquals("Astro Loop NPC walk speed must be full", 0.105f, npc.x, 0.001f)
    }

    @Test
    fun `npc walks at half speed in corruption mode`() {
        persistence.setStoryStageCode(StoryStage.CORRUPTION.code)
        assertFalse("Astro Loop must be off", StoryStateManager.isAstroLoop(persistence))

        val npc = WalkerNPC(pilotIndex = 0, color = 0xFF_FFFFFF.toInt(), x = 0f, targetX = 1f, walking = true, idleTimer = 0f)
        state.npcWalkers.add(npc)

        state.updateNPCWalkers(1.0f)

        // Corruption half-speed = 0.105 * 0.5 = 0.0525f
        assertEquals("Corruption NPC walk speed must be halved", 0.0525f, npc.x, 0.001f)
    }

    @Test
    fun `pilot walks at slow cinematic speed during intro`() {
        state.introCinematic = true

        state.pilotX = 0f
        state.pilotTargetX = 1000f
        state.pilotWalking = true

        state.updatePilotWalker(1.0f)

        // Cinematic speed = 0.20 * 1000 = 200px/s (slower than normal 700px/s)
        assertEquals("Intro cinematic walk speed must be 200px/s", 200f, state.pilotX, 1f)
    }

    @Test
    fun `initialize activates cinematic and starts on bar when intro not done`() {
        // resetAllProgress() in @Before leaves intro_cinematic_done cleared
        assertFalse(persistence.isIntroDone())

        state.initialize()

        assertTrue("Fresh install must activate the intro cinematic", state.introCinematic)
        assertEquals("Cinematic must start on the bar page", 0, state.currentPage)
    }

    @Test
    fun `initialize leaves cinematic off and starts on shipyard when intro done`() {
        persistence.setIntroDone()

        state.initialize()

        assertFalse("Completed intro must not re-activate the cinematic", state.introCinematic)
        assertEquals("Normal start is the shipyard page", 1, state.currentPage)
    }
}

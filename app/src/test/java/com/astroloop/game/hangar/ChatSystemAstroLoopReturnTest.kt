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
class ChatSystemAstroLoopReturnTest {

    private lateinit var persistence: PersistenceManager
    private lateinit var state: HangarState
    private lateinit var chatSystem: ChatSystem

    @Before
    fun setup() {
        persistence = PersistenceManager(ApplicationProvider.getApplicationContext())
        persistence.resetAllProgress()
        state = HangarState(persistence)
        chatSystem = ChatSystem()
    }

    @Test
    fun `first entry with no completed run produces no survived-time message`() {
        persistence.setStoryStageCode(StoryStage.ASTRO_LOOP.code)
        assertEquals(0f, persistence.getLastAstroRunSeconds(), 0.001f)
        state.addChatMessage("TOBAR", "stale line", 0xFF88AACC.toInt())

        chatSystem.onDeathReturn(state, "pilot_astro")

        assertEquals("First entry must clear chat and add no survived-time messages",
            0, state.chatMessages.size)
    }

    @Test
    fun `return after a completed run produces a survived-time message`() {
        persistence.setStoryStageCode(StoryStage.ASTRO_LOOP.code)
        persistence.setLastAstroRunSeconds(125f)
        persistence.updateAstroLoopBestSeconds(125f)

        chatSystem.onDeathReturn(state, "pilot_astro")
        drainConversation()

        assertTrue("A completed run must produce at least one message", state.chatMessages.isNotEmpty())
        assertTrue("Message must report survival time",
            state.chatMessages.any { it.text.contains("survived for") })
    }

    @Test
    fun `return burst is delivered line by line, never all at once`() {
        persistence.setStoryStageCode(StoryStage.ASTRO_LOOP.code)
        persistence.setLastAstroRunSeconds(125f)
        persistence.updateAstroLoopBestSeconds(125f)

        chatSystem.onDeathReturn(state, "pilot_astro")

        assertEquals("No line may render on the return frame itself", 0, state.chatMessages.size)
        assertNotNull("Lines must be queued as a conversation", state.activeConversation)

        // First tick past the initial delay delivers exactly one line
        chatSystem.update(ChatSystem.DEATH_RETURN_FIRST_LINE_DELAY + 0.1f, state)
        assertEquals("Exactly one line after the first delivery tick", 1, state.chatMessages.size)

        // Next line only lands after a full LINE_PAUSE, not sooner
        chatSystem.update(0.5f, state)
        assertEquals("Second line must wait out LINE_PAUSE", 1, state.chatMessages.size)
        chatSystem.update(ChatSystem.LINE_PAUSE, state)
        assertEquals(2, state.chatMessages.size)
    }

    /** Death-return lines are queued as a conversation — tick update() until delivered. */
    private fun drainConversation() {
        var guard = 0
        while (state.activeConversation != null && guard++ < 40) {
            chatSystem.update(ChatSystem.LINE_PAUSE + 0.1f, state)
        }
    }
}

package com.astroloop.game.hangar

import androidx.test.core.app.ApplicationProvider
import com.astroloop.game.core.StoryStage
import com.astroloop.game.core.StoryStateManager
import com.astroloop.game.data.BarConversations
import com.astroloop.game.data.PersistenceManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ChatSystemAstroLoopTest {

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
    fun `shield conversation queued on first update in astro loop mode without any prior run`() {
        persistence.setStoryStageCode(StoryStage.ASTRO_LOOP.code)
        assertFalse(persistence.isAstroLoopShieldConvoShown())
        assertNull(state.activeConversation)

        chatSystem.update(0.016f, state)

        assertNotNull("Shield conversation should queue on first bar load in Astro Loop mode", state.activeConversation)
        assertTrue("Flag should be set immediately when conversation is queued", persistence.isAstroLoopShieldConvoShown())
    }

    @Test
    fun `shield conversation not queued when flag already set`() {
        persistence.setStoryStageCode(StoryStage.ASTRO_LOOP.code)
        persistence.setAstroLoopShieldConvoShown()

        chatSystem.update(0.1f, state)

        assertNull("Shield conversation should not re-queue after flag is set", state.activeConversation)
    }

    @Test
    fun `shield conversation not queued when not in astro loop mode`() {
        assertFalse(StoryStateManager.isAstroLoop(persistence))

        chatSystem.update(0.1f, state)

        assertNull("Shield conversation should not queue outside Astro Loop mode", state.activeConversation)
    }

    @Test
    fun `shield conversation not queued when another conversation is already active`() {
        persistence.setStoryStageCode(StoryStage.ASTRO_LOOP.code)
        // Simulate onDeathReturn or some other system having queued a conversation
        val existingConvo = BarConversations.getShieldDiscoveryConversation()!!
        state.activeConversation = existingConvo.lines
        state.conversationLineIndex = 0
        state.conversationLineTimer = 4f

        // update() will process the active conversation and return early — not reach the trigger
        chatSystem.update(0.1f, state)

        assertFalse("Flag should not be set when trigger was skipped due to active conversation",
            persistence.isAstroLoopShieldConvoShown())
    }

    @Test
    fun `onDeathReturn sets 3f cooldown for normal mode`() {
        persistence.unlockPilot("pilot_medic")
        chatSystem.onDeathReturn(state, "pilot_medic")
        assertEquals("Normal mode post-return cooldown must be 3f", 3f, state.conversationCooldown, 0.001f)
    }

    @Test
    fun `onDeathReturn sets same 3f cooldown in astro loop mode`() {
        persistence.setStoryStageCode(StoryStage.ASTRO_LOOP.code)
        chatSystem.onDeathReturn(state, "pilot_medic")
        assertEquals("Astro Loop post-return cooldown must also be 3f — same rules as normal mode",
            3f, state.conversationCooldown, 0.001f)
    }

    @Test
    fun `second update call with active conversation does not start a new conversation`() {
        // Inject an in-progress conversation directly: the weighted picker starts
        // conversations probabilistically, so relying on update() to produce one would
        // make this guard flaky. The active-conversation delivery branch runs before the
        // picker, so a later tick must never replace an in-progress conversation.
        val firstConvo = listOf(
            com.astroloop.game.hangar.ChatMessage("Dash", "One.", 0xFFFFFFFF.toInt()),
            com.astroloop.game.hangar.ChatMessage("Dash", "Two.", 0xFFFFFFFF.toInt())
        )
        state.currentPage = 0
        state.activeConversation = firstConvo
        state.conversationLineIndex = 0
        state.conversationLineTimer = 5f  // not yet time to deliver the next line; stays active

        chatSystem.update(0.016f, state)

        assertSame("Update must not replace an in-progress conversation", firstConvo, state.activeConversation)
    }

    @Test
    fun `conversation cooldown is 20f in normal mode after conversation finishes`() {
        val fakeConvo = listOf(com.astroloop.game.hangar.ChatMessage("Dash", "Hey.", 0xFFFFFFFF.toInt()))
        state.activeConversation = fakeConvo
        state.conversationLineIndex = 1      // past the last line
        state.conversationLineTimer = -1f    // timer expired — triggers "finished" branch

        chatSystem.update(0.016f, state)

        assertNull("Conversation must be cleared after finishing", state.activeConversation)
        assertEquals("Normal mode conversation cooldown must be 20f",
            20f, state.conversationCooldown, 0.001f)
    }

    @Test
    fun `conversation cooldown is 20f in corruption mode after conversation finishes`() {
        persistence.setStoryStageCode(StoryStage.CORRUPTION.code)
        assertFalse("Astro Loop must be off", StoryStateManager.isAstroLoop(persistence))

        val fakeConvo = listOf(com.astroloop.game.hangar.ChatMessage("Dash", "Hey.", 0xFFFFFFFF.toInt()))
        state.activeConversation = fakeConvo
        state.conversationLineIndex = 1
        state.conversationLineTimer = -1f

        chatSystem.update(0.016f, state)

        assertNull("Conversation must be cleared", state.activeConversation)
        assertEquals("Corruption mode conversation cooldown must be 20f (same as normal, cooldown parity)",
            20f, state.conversationCooldown, 0.001f)
    }

    @Test
    fun `conversation cooldown is 20f in astro loop stage`() {
        persistence.setStoryStageCode(StoryStage.ASTRO_LOOP.code)
        persistence.setAstroLoopShieldConvoShown()  // prevent shield convo from firing

        val fakeConvo = listOf(com.astroloop.game.hangar.ChatMessage("Dash", "Hey.", 0xFFFFFFFF.toInt()))
        state.activeConversation = fakeConvo
        state.conversationLineIndex = 1
        state.conversationLineTimer = -1f

        chatSystem.update(0.016f, state)

        assertNull("Conversation must be cleared", state.activeConversation)
        assertEquals("Astro Loop conversation cooldown must be 20f (not corruption 45f)",
            20f, state.conversationCooldown, 0.001f)
    }

    @Test
    fun `onDeathReturn clears chat in corruption phase`() {
        persistence.setStoryStageCode(StoryStage.CORRUPTION.code)
        // Mark all non-Astro pilots dead so no attrition lines are added after the clear
        val nonAstroPilots = listOf(
            "pilot_medic", "pilot_rascal", "pilot_brutus", "pilot_frost", "pilot_dash",
            "pilot_ember", "pilot_fang", "pilot_kraken", "pilot_whiskers", "pilot_unit7", "pilot_havoc"
        )
        nonAstroPilots.forEach { persistence.addDeadPilot(it) }
        // Mark all dead pilots as mourned so no named death lines are added
        nonAstroPilots.forEach { persistence.addPilotMourned(it) }
        state.addChatMessage("MEDIC", "Some leftover line.", 0xFFFFFFFF.toInt())
        assertEquals(1, state.chatMessages.size)

        chatSystem.onDeathReturn(state, "pilot_medic")

        assertEquals("Chat must be cleared on return even in corruption phase", 0, state.chatMessages.size)
    }

    @Test
    fun `onDeathReturn in corruption with all crew dead adds no Astro lines`() {
        persistence.setStoryStageCode(StoryStage.CORRUPTION.code)
        val nonAstroPilots = listOf(
            "pilot_medic", "pilot_rascal", "pilot_brutus", "pilot_frost", "pilot_dash",
            "pilot_ember", "pilot_fang", "pilot_kraken", "pilot_whiskers", "pilot_unit7", "pilot_havoc"
        )
        nonAstroPilots.forEach { persistence.addDeadPilot(it) }

        chatSystem.onDeathReturn(state, "pilot_astro")

        assertEquals("No messages must appear when all crew dead in corruption bar", 0, state.chatMessages.size)
    }

    @Test
    fun `onFirstLaunch is a no-op in astro loop mode`() {
        persistence.setStoryStageCode(StoryStage.ASTRO_LOOP.code)

        chatSystem.onFirstLaunch(state)

        assertEquals("Fresh-install greeting must not appear in the Tobar bar",
            0, state.chatMessages.size)
        assertNull("Medic volunteer line must not queue in astro loop mode",
            state.activeConversation)
    }

    @Test
    fun `onFirstLaunch still greets and queues the medic outside astro loop mode`() {
        chatSystem.onFirstLaunch(state)

        assertEquals(1, state.chatMessages.size)
        assertEquals("TB-26", state.chatMessages[0].speaker)
        assertEquals("Welcome back, commander.", state.chatMessages[0].text)
        assertNotNull("Medic volunteer line must be queued", state.activeConversation)
    }

    @Test
    fun `bartender reactions use the TOBAR label in astro loop`() {
        persistence.setStoryStageCode(StoryStage.ASTRO_LOOP.code)

        chatSystem.onPilotHired(state, "RASCAL")

        assertEquals("TOBAR", state.chatMessages.last().speaker)
    }
}

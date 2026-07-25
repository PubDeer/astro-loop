package com.astroloop.game.hangar

import androidx.test.core.app.ApplicationProvider
import com.astroloop.game.data.PersistenceManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ChatPickerIntegrationTest {

    private lateinit var persistence: PersistenceManager
    private lateinit var chatSystem: ChatSystem

    @Before
    fun setup() {
        persistence = PersistenceManager(ApplicationProvider.getApplicationContext())
        persistence.resetAllProgress()
        chatSystem = ChatSystem()
    }

    @Test
    fun `idle one-liners do not recycle until run reset`() {
        persistence.unlockPilot("pilot_medic")
        val state = HangarState(persistence)
        state.currentPage = 0

        // 1-way is available at the start.
        assertTrue(chatSystem.oneWayAvailable(state, corrupted = false, onBarPage = true, selectedCallsign = null))

        // Drain the ambient pools (Medic + TB-26) by firing idle lines repeatedly.
        repeat(400) { chatSystem.addIdleLine(state, 1) }

        // With no auto-recycle, every ambient line is now spent -> 1-way no longer available.
        assertFalse(
            "ambient one-liners must not recycle within a visit",
            chatSystem.oneWayAvailable(state, corrupted = false, onBarPage = true, selectedCallsign = null)
        )

        // Run return clears tracking and restores availability.
        chatSystem.resetUsedLines()
        assertTrue(chatSystem.oneWayAvailable(state, corrupted = false, onBarPage = true, selectedCallsign = null))
    }

    @Test
    fun `bar page picker surfaces both idle lines and conversations`() {
        persistence.unlockAllShipsAndPilots()
        val state = HangarState(persistence)
        state.currentPage = 0
        state.selectedPilotIndex = 0

        var idle = 0
        var convo = 0
        repeat(150) {
            state.conversationCooldown = 0f
            state.activeConversation = null
            state.chatMessages.clear()
            chatSystem.update(0.016f, state)
            if (state.activeConversation != null) convo++ else if (state.chatMessages.isNotEmpty()) idle++
        }
        assertTrue("solo one-liners must fire on the bar page", idle > 0)
        assertTrue("conversations must also fire on the bar page", convo > 0)
    }

    @Test
    fun `off the bar page, conversations exclude the selected pilot`() {
        persistence.unlockAllShipsAndPilots()
        val state = HangarState(persistence)
        state.currentPage = 1  // shipyard
        // selectedPilotIndex set to Medic (index 0)
        state.selectedPilotIndex = 0

        var convosStarted = 0
        repeat(200) {
            state.conversationCooldown = 0f
            state.activeConversation = null
            chatSystem.update(0.016f, state)
            state.activeConversation?.let { lines ->
                convosStarted++
                // The started conversation must not include the selected pilot's callsign as a speaker.
                val selCallsign = com.astroloop.game.data.PilotDefinitions.getPilotByIndex(0)!!.callsign
                assertFalse(
                    "off-page conversation must not include the selected pilot",
                    lines.any { it.speaker == selCallsign }
                )
            }
        }
        assertTrue("conversations should still fire off the bar page", convosStarted > 0)
    }
}

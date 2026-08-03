package com.astroloop.game.hangar

import androidx.test.core.app.ApplicationProvider
import com.astroloop.game.data.PersistenceManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Post-run return chatter: TB's greeting comes first, and the bar picks back up quickly.
 *
 * addYenFromRun() calls onDeathReturn() and then checkPilotRecruitment() in the SAME frame,
 * so a recruitment that writes straight into the chat log would land its arrival line a full
 * DEATH_RETURN_FIRST_LINE_DELAY before the queued "Welcome back, commander."
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ChatSystemReturnOrderingTest {

    private lateinit var persistence: PersistenceManager
    private lateinit var state: HangarState
    private lateinit var chatSystem: ChatSystem

    private val welcome = "Welcome back, commander."
    private val rascalArrival = "New arrival. Watch your valuables."

    @Before
    fun setup() {
        persistence = PersistenceManager(ApplicationProvider.getApplicationContext())
        persistence.resetAllProgress()
        state = HangarState(persistence)
        chatSystem = ChatSystem()
    }

    @Test
    fun `greeting precedes a same-frame recruitment arrival line`() {
        // Same frame, same order as addYenFromRun()
        chatSystem.onDeathReturn(state, "pilot_medic")
        chatSystem.onPilotRecruited(state, "RASCAL")

        assertEquals("Nothing may render on the return frame itself", 0, state.chatMessages.size)
        drainConversation()

        val texts = state.chatMessages.map { it.text }
        assertTrue("Greeting must be present, got $texts", welcome in texts)
        assertTrue("Arrival line must be present, got $texts", rascalArrival in texts)
        assertEquals("Greeting must be the very first line, got $texts", welcome, texts.first())
        assertTrue(
            "Arrival must land after the greeting, got $texts",
            texts.indexOf(rascalArrival) > texts.indexOf(welcome)
        )
    }

    @Test
    fun `recruitment outside a return still speaks on its own`() {
        // The slot-machine jackpot path recruits with no burst in flight.
        chatSystem.onPilotRecruited(state, "RASCAL")
        drainConversation()

        assertEquals(
            listOf(rascalArrival, "Rude. Accurate, but rude."),
            state.chatMessages.map { it.text }
        )
    }

    @Test
    fun `the arrival answers TB, in TB's line and then the pilot's`() {
        chatSystem.onPilotRecruited(state, "RASCAL")
        drainConversation()

        val speakers = state.chatMessages.map { it.speaker }
        assertEquals("TB announces first, then the pilot answers",
            listOf("TB-26", "RASCAL"), speakers)
    }

    @Test
    fun `every pilot but Medic has a distinct line for each reachable loop`() {
        // getStoryLoop() clamps to 1..3, so those are the only reachable keys.
        for (pilot in com.astroloop.game.data.PilotDefinitions.pilots) {
            if (pilot.callsign == "MEDIC") continue
            val lines = (1..3).map { loop ->
                val line = chatSystem.arrivalResponseFor(pilot.callsign, loop)
                assertNotNull("${pilot.callsign} must resolve a line on loop $loop", line)
                line
            }
            assertEquals(
                "${pilot.callsign} should read differently each loop, got $lines",
                3, lines.toSet().size
            )
        }
    }

    @Test
    fun `an unauthored loop falls back to the loop 1 line`() {
        assertEquals(
            chatSystem.arrivalResponseFor("RASCAL", 1),
            chatSystem.arrivalResponseFor("RASCAL", 99)
        )
    }

    @Test
    fun `Medic has no arrival response`() {
        // She already volunteers over the empty roster in onFirstLaunch.
        assertNull(chatSystem.arrivalResponseFor("MEDIC", 1))
    }

    @Test
    fun `no pilot arrival response says commander`() {
        for (pilot in com.astroloop.game.data.PilotDefinitions.pilots) {
            for (loop in 1..3) {
                val line = chatSystem.arrivalResponseFor(pilot.callsign, loop) ?: continue
                assertFalse(
                    "\"commander\" is TB-26/Tobar's word only — ${pilot.callsign} loop $loop: $line",
                    line.contains("commander", ignoreCase = true)
                )
            }
        }
    }

    @Test
    fun `banter resumes on a short tail after a scripted burst`() {
        chatSystem.onDeathReturn(state, "pilot_medic")
        drainConversation()

        assertEquals(
            "A scripted burst must hand back the short tail, not the full gap",
            ChatSystem.SCRIPTED_BURST_TAIL_COOLDOWN, state.conversationCooldown, 0.001f
        )
        assertEquals(
            "The tail is one-shot — the next conversation gets the normal gap back",
            ChatSystem.CONVERSATION_COOLDOWN, state.conversationEndCooldown, 0.001f
        )
    }

    /** Tick until the queued burst has been fully delivered, stopping the frame it ends. */
    private fun drainConversation() {
        var guard = 0
        chatSystem.update(ChatSystem.DEATH_RETURN_FIRST_LINE_DELAY + 0.01f, state)
        while (state.activeConversation != null && guard++ < 40) {
            chatSystem.update(ChatSystem.LINE_PAUSE + 0.01f, state)
        }
        assertNull("Burst should have drained well inside the guard", state.activeConversation)
    }
}

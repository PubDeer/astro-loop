package com.astroloop.game.hangar

import androidx.test.core.app.ApplicationProvider
import com.astroloop.game.core.StoryStage
import com.astroloop.game.data.LoopDefinitions
import com.astroloop.game.data.PersistenceManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BandanaAwardCeremonyTest {

    private lateinit var persistence: PersistenceManager
    private lateinit var state: HangarState
    private lateinit var chat: ChatSystem

    @Before
    fun setup() {
        persistence = PersistenceManager(ApplicationProvider.getApplicationContext())
        persistence.resetAllProgress()
        persistence.setStoryStageCode(StoryStage.ASTRO_LOOP.code)
        persistence.setLastAstroRunSeconds(650f)
        persistence.updateAstroLoopBestSeconds(650f)
        state = HangarState(persistence)
        chat = ChatSystem()
    }

    /** Death-return lines are queued as a conversation — tick update() until delivered. */
    private fun drainConversation() {
        var guard = 0
        while (state.activeConversation != null && guard++ < 40) {
            chat.update(ChatSystem.LINE_PAUSE + 0.1f, state)
        }
    }

    @Test
    fun ceremonyAddsTobarFramingAndPilotReply() {
        persistence.addBandana("pilot_dash")          // count = 1 (< 12)
        persistence.setPendingBandanaPilot("pilot_dash")

        chat.onDeathReturn(state, "pilot_dash")

        assertTrue("Return burst must be queued, not dumped in one frame",
            state.chatMessages.isEmpty() && state.activeConversation != null)
        drainConversation()

        val texts = state.chatMessages.map { it.text }
        assertTrue("Tobar framing missing", texts.any { it in LoopDefinitions.tobarBandanaFraming })
        assertTrue("Pilot reply missing",
            texts.contains(LoopDefinitions.bandanaAwardReplies["pilot_dash"]))
        assertTrue("Guaranteed desert-town hint #1 missing",
            texts.contains(LoopDefinitions.desertTownHints[0]))
        assertNull("Pending must be cleared", persistence.getPendingBandanaPilot())
    }

    @Test
    fun ceremonyAddsTheScriptedHintForItsBandanaCount() {
        val ids = listOf(
            "pilot_medic", "pilot_rascal", "pilot_brutus", "pilot_frost",
            "pilot_dash", "pilot_ember", "pilot_fang"
        )
        for (id in ids) persistence.addBandana(id)    // count = 7
        persistence.setPendingBandanaPilot("pilot_fang")

        chat.onDeathReturn(state, "pilot_fang")
        drainConversation()

        val texts = state.chatMessages.map { it.text }
        assertTrue("Hint #7 must fire after the 7th bandana",
            texts.contains(LoopDefinitions.desertTownHints[6]))
        assertFalse("Only the 7th hint may fire",
            texts.any { it in LoopDefinitions.desertTownHints && it != LoopDefinitions.desertTownHints[6] })
    }

    @Test
    fun twelfthBandanaUsesTheSpecialBeat() {
        val ids = listOf(
            "pilot_medic", "pilot_rascal", "pilot_brutus", "pilot_frost",
            "pilot_dash", "pilot_ember", "pilot_fang", "pilot_kraken",
            "pilot_whiskers", "pilot_unit7", "pilot_havoc", "pilot_astro"
        )
        for (id in ids) persistence.addBandana(id)    // count = 12
        persistence.setPendingBandanaPilot("pilot_astro")

        chat.onDeathReturn(state, "pilot_astro")
        drainConversation()

        val texts = state.chatMessages.map { it.text }
        for (beat in LoopDefinitions.tobarTwelfthBandanaBeat) assertTrue("Missing beat: $beat", texts.contains(beat))
        assertTrue("Hint #12 closes the twelfth ceremony",
            texts.contains(LoopDefinitions.desertTownHints[11]))
    }

    @Test
    fun noPendingMeansNoCeremony() {
        chat.onDeathReturn(state, "pilot_dash")
        drainConversation()
        val texts = state.chatMessages.map { it.text }
        assertFalse(texts.any { it in LoopDefinitions.tobarBandanaFraming })
    }
}

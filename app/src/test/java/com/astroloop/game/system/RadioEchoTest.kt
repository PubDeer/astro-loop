package com.astroloop.game.system

import com.astroloop.game.core.GameState
import com.astroloop.game.data.RadioDefinitions
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RadioEchoTest {

    private val pilots = listOf(
        "pilot_medic", "pilot_rascal", "pilot_brutus", "pilot_frost",
        "pilot_dash", "pilot_ember", "pilot_fang", "pilot_kraken",
        "pilot_whiskers", "pilot_unit7", "pilot_havoc", "pilot_astro"
    )

    @Test
    fun everyPilotHasAnEchoLineWithinLimit() {
        for (id in pilots) {
            val lines = RadioDefinitions.getLines(id, "astro_echo", filterTb26 = true)
            assertNotNull("Missing astro_echo for $id", lines)
            assertTrue("Empty astro_echo for $id", lines!!.isNotEmpty())
            for (line in lines) assertTrue("Echo too long ($id): \"$line\"", line.length <= 35)
        }
    }

    @Test
    fun tenMinuteFiresEchoWhenPilotLacksBandana() {
        val state = GameState()
        state.astroLoopMode = true
        state.activePilotId = "pilot_dash"
        state.activePilotHasBandana = false
        RadioSystem().onTimeMilestone(state, 4)
        val echo = RadioDefinitions.getLines("pilot_dash", "astro_echo", filterTb26 = true)!!
        assertTrue("Expected an echo line, got: ${state.radioMessage}", state.radioMessage in echo)
    }

    @Test
    fun tenMinuteFiresCelebratoryWhenPilotOwnsBandana() {
        val state = GameState()
        state.astroLoopMode = true
        state.activePilotId = "pilot_dash"
        state.activePilotHasBandana = true
        RadioSystem().onTimeMilestone(state, 4)
        val celebratory = RadioDefinitions.getLines("pilot_dash", "time_10min", filterTb26 = true)!!
        assertTrue("Expected a time_10min line, got: ${state.radioMessage}", state.radioMessage in celebratory)
    }
}

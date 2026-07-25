package com.astroloop.game.system

import com.astroloop.game.core.GameState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ReckoningRadioSilenceTest {

    @Test
    fun ambientMessagesAreDroppedDuringTheReckoning() {
        val state = GameState()
        state.reckoningActive = true
        RadioSystem().showMessage(state, "DASH", "ambient line", 0xFFFFFFFF.toInt())
        assertNull(state.radioMessage)
    }

    @Test
    fun ambientMessagesShowOutsideTheReckoning() {
        val state = GameState()
        RadioSystem().showMessage(state, "DASH", "ambient line", 0xFFFFFFFF.toInt())
        assertEquals("ambient line", state.radioMessage)
    }

    @Test
    fun scriptedMessagesStillShowDuringTheReckoning() {
        val state = GameState()
        state.reckoningActive = true
        RadioSystem().showScriptedMessage(state, "ASTRO", "scripted line", 0xFFFFFFFF.toInt())
        assertEquals("scripted line", state.radioMessage)
    }

    @Test
    fun shieldsDownSpeaksNormallyButIsSilentDuringTheReckoning() {
        // Positive control first, so the silent case can't pass vacuously.
        val normal = GameState().apply { activePilotId = "pilot_dash" }
        RadioSystem().onShieldsDown(normal)
        assertNotNull("shields_down should fire outside the fight", normal.radioMessage)

        val fight = GameState().apply {
            activePilotId = "pilot_dash"
            reckoningActive = true
        }
        RadioSystem().onShieldsDown(fight)
        assertNull("shields_down leaked into the reckoning", fight.radioMessage)
    }
}

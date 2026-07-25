package com.astroloop.game.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FleetSystemEngineRestartTest {

    @Test
    fun `scale is zero during the sputter`() {
        assertEquals(0f, FleetSystem.engineRestartSpeedScale(0f), 0f)
        assertEquals(0f, FleetSystem.engineRestartSpeedScale(FleetSystem.ENGINE_SPUTTER_DURATION - 0.01f), 0f)
    }

    @Test
    fun `scale ramps linearly after the catch`() {
        val mid = FleetSystem.ENGINE_SPUTTER_DURATION + FleetSystem.ENGINE_RAMP_DURATION / 2f
        assertEquals(0.5f, FleetSystem.engineRestartSpeedScale(mid), 0.001f)
    }

    @Test
    fun `scale reaches and holds full at the end of the ramp`() {
        val end = FleetSystem.ENGINE_SPUTTER_DURATION + FleetSystem.ENGINE_RAMP_DURATION
        assertEquals(1f, FleetSystem.engineRestartSpeedScale(end), 0.001f)
        assertEquals(1f, FleetSystem.engineRestartSpeedScale(end + 5f), 0f)
    }

    @Test
    fun `fleet warp beat lets the engine restart fully play out`() {
        assertTrue(
            "fleet must not warp in before the sputter and ramp complete",
            FleetSystem.FLEET_WARP_BEAT >=
                FleetSystem.ENGINE_SPUTTER_DURATION + FleetSystem.ENGINE_RAMP_DURATION
        )
    }

    @Test
    fun `fleet warp beat is 4 seconds`() {
        assertEquals(4f, FleetSystem.FLEET_WARP_BEAT, 0.001f)
    }
}

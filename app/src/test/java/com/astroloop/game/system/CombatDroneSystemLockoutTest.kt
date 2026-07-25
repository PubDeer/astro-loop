package com.astroloop.game.system

import com.astroloop.game.core.GameState
import com.astroloop.game.entity.Ship
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reckoning bare-ship lockout: Astro's kit still holds the tb26 passive (droneCount ≥ 1),
 * but while passives are benched — the fight itself and the death retreat that follows —
 * the drone system must never spawn a wingman. Popping in alongside the emergency shield
 * was the observed bug.
 */
class CombatDroneSystemLockoutTest {

    private fun system(passivesDisabled: Boolean): CombatDroneSystem {
        val state = GameState()
        state.droneCount = 1
        state.hasDrone = true
        state.passivesDisabled = passivesDisabled
        return CombatDroneSystem(state, Ship())
    }

    @Test
    fun `no drone spawns while passives are locked out`() {
        val locked = system(passivesDisabled = true)
        locked.update(0.016f, emptyList(), emptyList())
        assertTrue("Bare-ship lockout must not spawn wingmen", locked.drones.isEmpty())
    }

    @Test
    fun `drone spawns normally when passives are enabled`() {
        val normal = system(passivesDisabled = false)
        normal.update(0.016f, emptyList(), emptyList())
        assertEquals(1, normal.drones.size)
    }
}

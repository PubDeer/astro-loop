package com.astroloop.game.weapon

import com.astroloop.game.core.GameState
import com.astroloop.game.weapon.weapons.HunterKiller
import org.junit.Assert.assertEquals
import org.junit.Test

class HunterKillerTest {

    @Test
    fun `double the cluster bomb fire rate — two beats at 120 bpm`() {
        val state = GameState()
        state.reset()
        assertEquals(1.0f, HunterKiller().getCooldown(state), 0.001f)
    }

    @Test
    fun `one torpedo per shot, duplicator core adds more`() {
        val state = GameState()
        state.reset()
        val weapon = HunterKiller()
        assertEquals(1, weapon.getProjectileCount(state))
        state.extraProjectiles = 1
        assertEquals(2, weapon.getProjectileCount(state))
    }

    @Test
    fun `id and name sell the rename`() {
        val weapon = HunterKiller()
        assertEquals("hunter_killer", weapon.id)
        assertEquals("Hunter-Killer", weapon.name)
    }
}

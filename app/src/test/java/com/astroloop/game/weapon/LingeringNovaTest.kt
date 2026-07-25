package com.astroloop.game.weapon

import com.astroloop.game.core.GameState
import com.astroloop.game.entity.EntityPool
import com.astroloop.game.entity.Firer
import com.astroloop.game.entity.Projectile
import com.astroloop.game.util.Vector2
import com.astroloop.game.weapon.weapons.LingeringNova
import org.junit.Assert.assertEquals
import org.junit.Test

class LingeringNovaTest {

    private fun stubFirer() = object : Firer {
        override val position = Vector2(0f, 0f)
        override val velocity = Vector2(0f, 0f)
        override val rotation = 0f
        override val radius = 12f
        override val isEnemyFirer = false
    }

    private fun freshState() = GameState().also { it.reset() }

    @Test
    fun `fire spawns 8 ring projectiles plus one visual-only core`() {
        val state = freshState()
        val pool = EntityPool({ Projectile() }, 50)
        val weapon = LingeringNova().also { it.level = 1 }

        weapon.fire(stubFirer(), state, pool, emptyList())

        val active = pool.getActiveEntities()
        assertEquals(9, active.size)
        assertEquals(1, active.count { it.isVisualOnly })
        assertEquals(8, active.count { !it.isVisualOnly })
    }

    @Test
    fun `pending second detonation fires one cooldown later`() {
        val state = freshState()
        val pool = EntityPool({ Projectile() }, 50)
        val weapon = LingeringNova().also { it.level = 1 }

        weapon.fire(stubFirer(), state, pool, emptyList())
        assertEquals(9, pool.activeCount())

        weapon.updatePending(weapon.getCooldown(state) + 0.1f, pool, state)
        assertEquals(17, pool.activeCount())
    }

    @Test
    fun `updatePending does nothing before the timer elapses`() {
        val state = freshState()
        val pool = EntityPool({ Projectile() }, 50)
        val weapon = LingeringNova().also { it.level = 1 }

        weapon.fire(stubFirer(), state, pool, emptyList())
        weapon.updatePending(0.5f, pool, state)
        assertEquals(9, pool.activeCount())
    }
}

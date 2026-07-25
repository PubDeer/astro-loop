package com.astroloop.game.entity

import com.astroloop.game.core.GameConfig
import org.junit.Assert.*
import org.junit.Test

class VisualEffectManagerTest {

    @Test
    fun `addPhoenixShockwave creates effect that expands to max radius over lifetime`() {
        val manager = VisualEffectManager()
        manager.addPhoenixShockwave(100f, 200f)

        val effects = manager.getEffects()
        assertEquals(1, effects.size)
        val effect = effects[0]
        assertEquals(EffectType.PHOENIX_SHOCKWAVE, effect.type)
        assertEquals(0f, effect.radius, 0.01f)
        assertEquals(GameConfig.PHOENIX_SHOCKWAVE_MAX_RADIUS, effect.maxRadius, 0.01f)
        assertEquals(GameConfig.PHOENIX_SHOCKWAVE_DURATION, effect.lifetime, 0.01f)

        // After half the lifetime, radius should be ~350f
        manager.update(0.3f)
        val updated = manager.getEffects()[0]
        assertEquals(GameConfig.PHOENIX_SHOCKWAVE_MAX_RADIUS / 2f, updated.radius, 1f)
    }

    @Test
    fun `phoenix shockwave effect is removed after lifetime`() {
        val manager = VisualEffectManager()
        manager.addPhoenixShockwave(0f, 0f)
        manager.update(0.7f) // past 0.6s lifetime
        assertTrue(manager.getEffects().isEmpty())
    }

    @Test
    fun `damage numbers are capped at 50 entries`() {
        val manager = VisualEffectManager()
        repeat(60) { manager.addDamageNumber(0f, 0f, 10) }
        assertTrue(manager.getDamageNumbers().size <= 50)
    }

    @Test
    fun `addDeathBlast enqueues a flash, multiple explosion rings, and a shockwave`() {
        val manager = VisualEffectManager()
        manager.addDeathBlast(10f, 20f)
        val effects = manager.getEffects()
        assertTrue("expected a layered blast", effects.size >= 5)
        assertTrue(effects.any { it.type == EffectType.HIT_FLASH })
        assertTrue(effects.any { it.type == EffectType.BOSS_SHOCKWAVE })
        assertEquals(3, effects.count { it.type == EffectType.EXPLOSION })
    }

    @Test
    fun `death blast rings outlive a normal explosion`() {
        val manager = VisualEffectManager()
        manager.addDeathBlast(0f, 0f)
        val rings = manager.getEffects().filter { it.type == EffectType.EXPLOSION }
        assertTrue("death rings must linger longer than 0.4s", rings.all { it.lifetime > 0.4f })
    }
}

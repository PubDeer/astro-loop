package com.astroloop.game.entity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectileVisualOnlyTest {

    @Test
    fun `isVisualOnly defaults to false`() {
        assertFalse(Projectile().isVisualOnly)
    }

    @Test
    fun `reset clears isVisualOnly`() {
        val p = Projectile()
        p.isVisualOnly = true
        assertTrue(p.isVisualOnly)
        p.reset()
        assertFalse(p.isVisualOnly)
    }
}

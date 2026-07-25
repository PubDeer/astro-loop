package com.astroloop.game.system

import com.astroloop.game.entity.Ship
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class FleetSystemScatterTest {

    @Test
    fun `scatter pushes velocity directly away from the source`() {
        val target = Ship()
        target.position.set(100f, 0f)   // 100px to the +x of the source at origin
        target.velocity.zero()
        FleetSystem.scatterEntity(target, 0f, 0f)
        // Impulse is the full EMP_SCATTER_IMPULSE along +x (started from rest)
        assertEquals(FleetSystem.EMP_SCATTER_IMPULSE, target.velocity.x, 0.01f)
        assertEquals(0f, target.velocity.y, 0.01f)
    }

    @Test
    fun `scatter adds a rotation tumble within the expected band`() {
        val target = Ship()
        target.position.set(0f, 50f)
        val before = target.rotation
        FleetSystem.scatterEntity(target, 0f, 0f)
        val delta = abs(target.rotation - before)
        assertTrue("tumble magnitude $delta must be in [1.5, 3.0]", delta in 1.5f..3.0f)
    }

    @Test
    fun `scatter is safe when target sits on the source`() {
        val target = Ship()
        target.position.set(0f, 0f)
        target.velocity.zero()
        FleetSystem.scatterEntity(target, 0f, 0f)   // distance coerced, no NaN
        assertTrue(target.velocity.x.isFinite() && target.velocity.y.isFinite())
        val speed = sqrt(target.velocity.x * target.velocity.x + target.velocity.y * target.velocity.y)
        assertTrue("impulse magnitude is bounded", speed <= FleetSystem.EMP_SCATTER_IMPULSE + 0.01f)
    }
}

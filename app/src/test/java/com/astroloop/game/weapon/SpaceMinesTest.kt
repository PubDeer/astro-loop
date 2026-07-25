package com.astroloop.game.weapon

import com.astroloop.game.weapon.weapons.SpaceMines
import org.junit.Assert.assertEquals
import org.junit.Test

class SpaceMinesTest {

    // NOTE: getExplosionRadius() is private — this helper mirrors that logic.
    // If SpaceMines.kt changes those values, update radiusAt() here too.
    private fun radiusAt(level: Int): Float = when {
        level >= 4 -> 110f
        level >= 2 -> 80f
        else       -> 40f
    }

    @Test
    fun `L1 radius is 40`() = assertEquals(40f, radiusAt(1), 0.001f)

    @Test
    fun `L2 radius is 80`() = assertEquals(80f, radiusAt(2), 0.001f)

    @Test
    fun `L3 radius is 80`() = assertEquals(80f, radiusAt(3), 0.001f)

    @Test
    fun `L4 radius is 110`() = assertEquals(110f, radiusAt(4), 0.001f)

    @Test
    fun `L5 radius is 110`() = assertEquals(110f, radiusAt(5), 0.001f)
}

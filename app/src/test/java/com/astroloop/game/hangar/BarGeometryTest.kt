// app/src/test/java/com/astroloop/game/hangar/BarGeometryTest.kt
package com.astroloop.game.hangar

import org.junit.Assert.assertEquals
import org.junit.Test

class BarGeometryTest {

    private val walkwayY = 1285.2f  // DESIGN_HEIGHT(2142) * 0.60

    @Test
    fun `counter top is 14px above the walkway (slim slab)`() {
        val g = BarGeometry(walkwayY)
        assertEquals(walkwayY - 14f, g.counterTop, 0.001f)
    }

    @Test
    fun `slab is 16px tall`() {
        val g = BarGeometry(walkwayY)
        assertEquals(16f, g.counterBottom - g.counterTop, 0.001f)
    }

    @Test
    fun `shelf and lamps ride the counter top`() {
        val g = BarGeometry(walkwayY)
        assertEquals(g.counterTop - 18f, g.shelfY, 0.001f)
        assertEquals(g.counterTop - 25f, g.lampY, 0.001f)
    }

    @Test
    fun `stool seat sits just under the counter top`() {
        val g = BarGeometry(walkwayY)
        assertEquals(g.counterTop + 3f, g.stoolSeatY, 0.001f)
    }

    @Test
    fun `beer slides flush on the counter top (no 2px sink)`() {
        val g = BarGeometry(walkwayY)
        assertEquals(g.counterTop, g.beerSurfaceY, 0.001f)
    }
}

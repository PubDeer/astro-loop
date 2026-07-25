package com.astroloop.game.util

import org.junit.Assert.assertEquals
import org.junit.Test

class GeometryTest {

    @Test
    fun `perpendicular distance to segment`() {
        assertEquals(5f, Geometry.distancePointToSegment(5f, 5f, 0f, 0f, 10f, 0f), 0.001f)
    }

    @Test
    fun `distance clamps to segment end`() {
        assertEquals(5f, Geometry.distancePointToSegment(15f, 0f, 0f, 0f, 10f, 0f), 0.001f)
    }

    @Test
    fun `zero-length segment degenerates to point distance`() {
        assertEquals(5f, Geometry.distancePointToSegment(3f, 4f, 0f, 0f, 0f, 0f), 0.001f)
    }
}

package com.astroloop.game.util

import com.astroloop.game.util.RetreatSteering.Obstacle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetreatSteeringTest {

    private val south = RetreatSteering.SOUTH

    @Test
    fun `no obstacles steers due south`() {
        val h = RetreatSteering.desiredHeading(0f, 0f, 16f, emptyList())
        assertEquals(south, h, 0.0001f)
    }

    @Test
    fun `obstacle beyond look-ahead is ignored`() {
        val far = listOf(Obstacle(0f, 5000f, 30f))
        val h = RetreatSteering.desiredHeading(0f, 0f, 16f, far, lookAhead = 250f)
        assertEquals(south, h, 0.0001f)
    }

    @Test
    fun `obstacle behind is ignored`() {
        val behind = listOf(Obstacle(0f, -100f, 30f))
        val h = RetreatSteering.desiredHeading(0f, 0f, 16f, behind)
        assertEquals(south, h, 0.0001f)
    }

    @Test
    fun `obstacle laterally clear is ignored`() {
        val aside = listOf(Obstacle(500f, 100f, 30f))
        val h = RetreatSteering.desiredHeading(0f, 0f, 16f, aside)
        assertEquals(south, h, 0.0001f)
    }

    @Test
    fun `obstacle ahead on the right veers left (heading greater than south)`() {
        val right = listOf(Obstacle(10f, 80f, 30f))
        val h = RetreatSteering.desiredHeading(0f, 0f, 16f, right)
        assertTrue("expected heading > south, was $h", h > south)
    }

    @Test
    fun `obstacle ahead on the left veers right (heading less than south)`() {
        val left = listOf(Obstacle(-10f, 80f, 30f))
        val h = RetreatSteering.desiredHeading(0f, 0f, 16f, left)
        assertTrue("expected heading < south, was $h", h < south)
    }
}

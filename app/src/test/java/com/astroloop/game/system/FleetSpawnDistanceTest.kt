package com.astroloop.game.system

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class FleetSpawnDistanceTest {

    @Test
    fun `spawn distance clears the viewport on a phone`() {
        val w = 1080f; val h = 2400f
        val halfDiagonal = 0.5f * sqrt(w * w + h * h)
        val dist = FleetSystem.tb26SpawnDistance(w, h)
        // Worst-case on-screen reach from the centered ship is the half-diagonal.
        assertTrue("must clear the half-diagonal", dist > halfDiagonal)
        assertTrue("full margin applied past the edge",
            dist >= halfDiagonal + FleetSystem.FLEET_EDGE_MARGIN)
    }

    @Test
    fun `spawn distance clears the viewport on an unfolded foldable`() {
        val w = 2208f; val h = 1840f // wide aspect
        val halfDiagonal = 0.5f * sqrt(w * w + h * h)
        val dist = FleetSystem.tb26SpawnDistance(w, h)
        assertTrue("must clear the half-diagonal on wide aspect", dist > halfDiagonal)
    }

    @Test
    fun `a larger viewport yields a larger spawn distance`() {
        val phone = FleetSystem.tb26SpawnDistance(1080f, 2400f)
        val foldable = FleetSystem.tb26SpawnDistance(2208f, 1840f) // larger diagonal
        assertTrue(foldable > phone)
    }

    @Test
    fun `solo return spawn offset clears the viewport on a phone`() {
        val w = 1080f; val h = 2400f
        val (dx, dy) = FleetSystem.tb26ReturnSpawnOffset(w, h)
        val halfDiagonal = 0.5f * sqrt(w * w + h * h)
        assertTrue("return spawn must start off-screen",
            sqrt(dx * dx + dy * dy) > halfDiagonal)
        assertTrue("full margin applied past the edge (sub-pixel rounding tolerated)",
            sqrt(dx * dx + dy * dy) >= halfDiagonal + FleetSystem.FLEET_EDGE_MARGIN - 0.01f)
    }

    @Test
    fun `solo return keeps the legacy forward-right approach bearing`() {
        val (dx, dy) = FleetSystem.tb26ReturnSpawnOffset(1080f, 2400f)
        assertTrue("approach from the player's right", dx > 0f)
        assertTrue("approach from above", dy < 0f)
    }
}

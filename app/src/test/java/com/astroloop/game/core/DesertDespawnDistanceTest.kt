package com.astroloop.game.core

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class DesertDespawnDistanceTest {

    // Worst-case horizontal separation inside the ±400 canyon corridor:
    // enemy at one wall, player at the other.
    private val maxCorridorDx = 800f

    /** Farthest possible spawn distance from the centered player for a given viewport height. */
    private fun worstCaseSpawnDistance(screenHeight: Float): Float {
        val dy = screenHeight * (0.5f +
            GameSurfaceView.DESERT_SPAWN_BAND_NEAR +
            GameSurfaceView.DESERT_SPAWN_BAND_SPREAD)
        return sqrt(dy * dy + maxCorridorDx * maxCorridorDx)
    }

    @Test
    fun `fresh spawns survive the despawn cull on a phone`() {
        val h = 2400f
        assertTrue(
            "spawn band must sit inside the despawn radius",
            worstCaseSpawnDistance(h) < GameSurfaceView.desertDespawnDistance(h)
        )
    }

    @Test
    fun `fresh spawns survive the despawn cull on a tall flagship`() {
        val h = 3120f
        assertTrue(
            "spawn band must sit inside the despawn radius on tall screens",
            worstCaseSpawnDistance(h) < GameSurfaceView.desertDespawnDistance(h)
        )
    }

    @Test
    fun `fresh spawns survive the despawn cull on a small screen`() {
        val h = 1280f
        assertTrue(
            "spawn band must sit inside the despawn radius on small screens",
            worstCaseSpawnDistance(h) < GameSurfaceView.desertDespawnDistance(h)
        )
    }

    @Test
    fun `nothing visible on screen can be despawned`() {
        val w = 1080f
        val h = 2400f
        val halfDiagonal = 0.5f * sqrt(w * w + h * h)
        assertTrue(
            "despawn radius must clear the visible half-diagonal",
            GameSurfaceView.desertDespawnDistance(h) > halfDiagonal
        )
    }

    @Test
    fun `a taller viewport yields a larger despawn radius`() {
        assertTrue(
            GameSurfaceView.desertDespawnDistance(3120f) >
                GameSurfaceView.desertDespawnDistance(2142f)
        )
    }
}

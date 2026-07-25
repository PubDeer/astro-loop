package com.astroloop.game.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesertSignatureGateTest {

    private fun onScreen(worldY: Float, cameraY: Float, screenHeight: Float): Boolean {
        val screenY = worldY - cameraY
        return screenY > 0f && screenY < screenHeight
    }

    private fun gateOpen(visible: Boolean, timer: Float, fallback: Float): Boolean =
        visible || timer >= fallback

    @Test
    fun `target above the camera is not on screen`() {
        assertFalse(onScreen(worldY = -50f, cameraY = 0f, screenHeight = 1000f))
    }

    @Test
    fun `target inside the camera is on screen`() {
        assertTrue(onScreen(worldY = 500f, cameraY = 0f, screenHeight = 1000f))
    }

    @Test
    fun `target below the camera is not on screen`() {
        assertFalse(onScreen(worldY = 1500f, cameraY = 0f, screenHeight = 1000f))
    }

    @Test
    fun `gate stays closed while invisible and before fallback`() {
        assertFalse(gateOpen(visible = false, timer = 3f, fallback = 15f))
    }

    @Test
    fun `gate opens as soon as a target is visible`() {
        assertTrue(gateOpen(visible = true, timer = 0f, fallback = 15f))
    }

    @Test
    fun `gate opens on the fallback timer even if nothing is visible`() {
        assertTrue(gateOpen(visible = false, timer = 15f, fallback = 15f))
    }
}

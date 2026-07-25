package com.astroloop.game.core

import com.astroloop.game.entity.Ship

class Camera {
    var x: Float = 0f
    var y: Float = 0f

    private var screenWidth: Float = 0f
    private var screenHeight: Float = 0f

    fun initialize(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
    }

    fun update(ship: Ship) {
        // Center camera on ship
        x = ship.position.x - screenWidth / 2
        y = ship.position.y - screenHeight / 2
    }

    fun toScreenX(worldX: Float): Float = worldX - x
    fun toScreenY(worldY: Float): Float = worldY - y

    fun toWorldX(screenX: Float): Float = screenX + x
    fun toWorldY(screenY: Float): Float = screenY + y

    fun getScreenWidth(): Float = screenWidth
    fun getScreenHeight(): Float = screenHeight

    // Get the visible world bounds
    fun getVisibleLeft(): Float = x
    fun getVisibleRight(): Float = x + screenWidth
    fun getVisibleTop(): Float = y
    fun getVisibleBottom(): Float = y + screenHeight

    // Check if a point is visible on screen (with margin)
    fun isVisible(worldX: Float, worldY: Float, margin: Float = 0f): Boolean {
        return worldX >= x - margin &&
               worldX <= x + screenWidth + margin &&
               worldY >= y - margin &&
               worldY <= y + screenHeight + margin
    }

    // Check if entity is too far from camera (for despawning)
    fun isTooFar(worldX: Float, worldY: Float, maxDistance: Float): Boolean {
        val centerX = x + screenWidth / 2
        val centerY = y + screenHeight / 2
        val dx = worldX - centerX
        val dy = worldY - centerY
        return dx * dx + dy * dy > maxDistance * maxDistance
    }

    fun reset() {
        x = 0f
        y = 0f
    }
}

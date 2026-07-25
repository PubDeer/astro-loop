package com.astroloop.game.entity

import com.astroloop.game.util.Vector2

abstract class Entity {
    val position = Vector2()
    val velocity = Vector2()
    var rotation: Float = 0f
    var rotationSpeed: Float = 0f
    var radius: Float = 10f
    var isActive: Boolean = true
    var health: Float = 1f
    var maxHealth: Float = 1f
    var cryoAffected: Boolean = false

    open fun update(deltaTime: Float) {
        position.add(velocity.x * deltaTime, velocity.y * deltaTime)
        rotation += rotationSpeed * deltaTime
    }

    open fun takeDamage(amount: Float): Boolean {
        health -= amount
        return health <= 0
    }

    open fun reset() {
        position.zero()
        velocity.zero()
        rotation = 0f
        rotationSpeed = 0f
        isActive = true
        health = maxHealth
        cryoAffected = false
    }

    fun collidesWith(other: Entity): Boolean {
        val dx = position.x - other.position.x
        val dy = position.y - other.position.y
        val distSq = dx * dx + dy * dy
        val radiusSum = radius + other.radius
        return distSq <= radiusSum * radiusSum
    }

    fun distanceTo(other: Entity): Float {
        return position.distance(other.position)
    }

    fun distanceToSquared(other: Entity): Float {
        return position.distanceSquared(other.position)
    }

    fun wrapAround(screenWidth: Float, screenHeight: Float) {
        if (position.x <= -radius) position.x = screenWidth + radius
        else if (position.x >= screenWidth + radius) position.x = -radius
        if (position.y <= -radius) position.y = screenHeight + radius
        else if (position.y >= screenHeight + radius) position.y = -radius
    }

    fun isOffScreen(screenWidth: Float, screenHeight: Float, margin: Float = 0f): Boolean {
        return position.x <= -radius - margin ||
               position.x >= screenWidth + radius + margin ||
               position.y <= -radius - margin ||
               position.y >= screenHeight + radius + margin
    }
}

package com.astroloop.game.util

import kotlin.math.*

data class Vector2(var x: Float = 0f, var y: Float = 0f) {

    fun set(x: Float, y: Float): Vector2 {
        this.x = x
        this.y = y
        return this
    }

    fun set(other: Vector2): Vector2 {
        this.x = other.x
        this.y = other.y
        return this
    }

    fun add(other: Vector2): Vector2 {
        x += other.x
        y += other.y
        return this
    }

    fun add(dx: Float, dy: Float): Vector2 {
        x += dx
        y += dy
        return this
    }

    fun sub(other: Vector2): Vector2 {
        x -= other.x
        y -= other.y
        return this
    }

    fun mul(scalar: Float): Vector2 {
        x *= scalar
        y *= scalar
        return this
    }

    fun divAssign(scalar: Float): Vector2 {
        if (scalar != 0f) {
            x /= scalar
            y /= scalar
        }
        return this
    }

    fun length(): Float = sqrt(x * x + y * y)

    fun lengthSquared(): Float = x * x + y * y

    fun normalize(): Vector2 {
        val len = length()
        if (len > 0f) {
            x /= len
            y /= len
        }
        return this
    }

    fun normalized(): Vector2 = copy().normalize()

    fun dot(other: Vector2): Float = x * other.x + y * other.y

    fun cross(other: Vector2): Float = x * other.y - y * other.x

    fun distance(other: Vector2): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }

    fun distanceSquared(other: Vector2): Float {
        val dx = x - other.x
        val dy = y - other.y
        return dx * dx + dy * dy
    }

    fun angle(): Float = atan2(y, x)

    fun angleTo(other: Vector2): Float = atan2(other.y - y, other.x - x)

    fun rotate(radians: Float): Vector2 {
        val cos = cos(radians)
        val sin = sin(radians)
        val newX = x * cos - y * sin
        val newY = x * sin + y * cos
        x = newX
        y = newY
        return this
    }

    fun rotated(radians: Float): Vector2 = copy().rotate(radians)

    fun lerp(target: Vector2, t: Float): Vector2 {
        x += (target.x - x) * t
        y += (target.y - y) * t
        return this
    }

    fun clampLength(maxLength: Float): Vector2 {
        val len = length()
        if (len > maxLength) {
            mul(maxLength / len)
        }
        return this
    }

    fun perpendicular(): Vector2 = Vector2(-y, x)

    fun zero(): Vector2 {
        x = 0f
        y = 0f
        return this
    }

    fun isZero(): Boolean = x == 0f && y == 0f

    operator fun plus(other: Vector2): Vector2 = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2): Vector2 = Vector2(x - other.x, y - other.y)
    operator fun times(scalar: Float): Vector2 = Vector2(x * scalar, y * scalar)
    operator fun div(scalar: Float): Vector2 = if (scalar != 0f) Vector2(x / scalar, y / scalar) else copy()
    operator fun unaryMinus(): Vector2 = Vector2(-x, -y)

    companion object {
        fun fromAngle(radians: Float, length: Float = 1f): Vector2 {
            return Vector2(cos(radians) * length, sin(radians) * length)
        }

        fun random(maxX: Float, maxY: Float): Vector2 {
            return Vector2(
                kotlin.random.Random.nextFloat() * maxX,
                kotlin.random.Random.nextFloat() * maxY
            )
        }

        fun randomUnit(): Vector2 {
            val angle = kotlin.random.Random.nextFloat() * 2f * PI.toFloat()
            return fromAngle(angle)
        }
    }
}

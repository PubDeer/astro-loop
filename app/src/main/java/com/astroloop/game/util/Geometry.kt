package com.astroloop.game.util

import kotlin.math.sqrt

object Geometry {
    /** Shortest distance from point (px,py) to segment (ax,ay)-(bx,by). */
    fun distancePointToSegment(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val abx = bx - ax
        val aby = by - ay
        val lenSq = abx * abx + aby * aby
        if (lenSq <= 0f) {
            val dx = px - ax
            val dy = py - ay
            return sqrt(dx * dx + dy * dy)
        }
        val t = (((px - ax) * abx + (py - ay) * aby) / lenSq).coerceIn(0f, 1f)
        val cx = ax + abx * t
        val cy = ay + aby * t
        val dx = px - cx
        val dy = py - cy
        return sqrt(dx * dx + dy * dy)
    }
}

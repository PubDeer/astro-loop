package com.astroloop.game.util

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Pure steering for the Astro Loop retreat autopilot: head south, weaving around
 * asteroids in the forward path with ordinary "point and thrust" steering.
 */
object RetreatSteering {

    /** Heading for due south (positive Y is down/south in world space). */
    const val SOUTH = (PI / 2).toFloat()

    /** Max deflection off south when an asteroid is right in front. */
    private val MAX_VEER = (PI / 3).toFloat()

    /** Extra lateral margin (px) added to the collision corridor. */
    private const val CORRIDOR_MARGIN = 30f

    data class Obstacle(val x: Float, val y: Float, val radius: Float)

    /**
     * Desired heading (radians). South by default; veers away from the nearest
     * asteroid that lies ahead (south) and inside the collision corridor. Veer
     * grows as the obstacle gets closer.
     */
    fun desiredHeading(
        shipX: Float,
        shipY: Float,
        shipRadius: Float,
        obstacles: List<Obstacle>,
        lookAhead: Float = 250f
    ): Float {
        var threat: Obstacle? = null
        var bestDist = Float.MAX_VALUE
        for (o in obstacles) {
            val dy = o.y - shipY            // positive = ahead (south)
            if (dy <= 0f) continue          // behind or level — ignore
            val dx = o.x - shipX
            val dist = sqrt(dx * dx + dy * dy)
            if (dist > lookAhead) continue
            val corridor = o.radius + shipRadius + CORRIDOR_MARGIN
            if (abs(dx) > corridor) continue  // not in the forward path
            if (dist < bestDist) {
                bestDist = dist
                threat = o
            }
        }

        val t = threat ?: return SOUTH
        val dx = t.x - shipX
        val proximity = (1f - bestDist / lookAhead).coerceIn(0f, 1f)
        val veer = MAX_VEER * proximity
        // Threat on the right (dx >= 0) -> veer left (heading > south); else right.
        return if (dx >= 0f) SOUTH + veer else SOUTH - veer
    }
}

package com.astroloop.game.util

import kotlin.math.sqrt

/** Axis-aligned 2D collision helpers. */
object Collision2D {

    /**
     * Push a circle out of an axis-aligned box when they overlap.
     *
     * Returns the corrected (x, y) for the circle's center. No overlap -> the
     * input center is returned unchanged. Center fully inside the box -> ejected
     * along the shallowest face.
     */
    fun resolveCircleOutOfAabb(
        cx: Float, cy: Float, r: Float,
        minX: Float, minY: Float, maxX: Float, maxY: Float
    ): Pair<Float, Float> {
        val closestX = cx.coerceIn(minX, maxX)
        val closestY = cy.coerceIn(minY, maxY)
        val dx = cx - closestX
        val dy = cy - closestY
        val dist2 = dx * dx + dy * dy

        if (dist2 >= r * r) return cx to cy            // no overlap

        if (dist2 > 0f) {                               // edge/corner overlap, center outside
            val dist = sqrt(dist2)
            val push = r - dist
            return (cx + dx / dist * push) to (cy + dy / dist * push)
        }

        // center inside the box — eject along the shallowest face
        val toLeft = cx - minX
        val toRight = maxX - cx
        val toTop = cy - minY
        val toBottom = maxY - cy
        return when (minOf(toLeft, toRight, toTop, toBottom)) {
            toLeft -> (minX - r) to cy
            toRight -> (maxX + r) to cy
            toTop -> cx to (minY - r)
            else -> cx to (maxY + r)
        }
    }
}

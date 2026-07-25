package com.astroloop.game.entity

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Re-entry burn state for a rushing ship (boss in the normal run, player in the
 * corruption run): afterimage ghost chain + shed embers. Pure state — drawn by
 * VectorRenderer.renderReentryBurn. Ghosts/sparks keep fading after emission
 * stops, satisfying the no-instant-disappearance rule.
 */
class ReentryBurn {
    companion object {
        const val GHOST_LIFETIME = 0.35f
        const val GHOST_SAMPLE_INTERVAL = 0.05f
        const val MAX_GHOSTS = 8
        const val SPARK_INTERVAL = 0.04f
        const val SPARK_LIFETIME = 0.3f
        const val EMBER_COLOR = 0xFFFF8833.toInt()
    }

    data class Ghost(var x: Float, var y: Float, var rotation: Float, var age: Float = 0f)

    val ghosts = mutableListOf<Ghost>()
    val sparks = mutableListOf<SawSpark>()
    private var ghostTimer = 0f
    private var sparkTimer = 0f

    fun hasContent(): Boolean = ghosts.isNotEmpty() || sparks.isNotEmpty()

    fun update(x: Float, y: Float, rotation: Float, deltaTime: Float, emitting: Boolean) {
        // Age and cull ghosts
        val gi = ghosts.iterator()
        while (gi.hasNext()) {
            val g = gi.next()
            g.age += deltaTime
            if (g.age >= GHOST_LIFETIME) gi.remove()
        }
        // Age, move, and cull sparks (SawSpark carries no integrator of its own)
        val si = sparks.iterator()
        while (si.hasNext()) {
            val s = si.next()
            s.x += s.vx * deltaTime
            s.y += s.vy * deltaTime
            s.age += deltaTime
            if (s.age >= s.lifetime) si.remove()
        }
        if (!emitting) {
            ghostTimer = 0f
            sparkTimer = 0f
            return
        }
        // Sample a ghost every GHOST_SAMPLE_INTERVAL
        ghostTimer += deltaTime
        if (ghostTimer >= GHOST_SAMPLE_INTERVAL) {
            ghostTimer = 0f
            ghosts.add(Ghost(x, y, rotation))
            if (ghosts.size > MAX_GHOSTS) ghosts.removeAt(0)
        }
        // Shed embers backwards off the hull
        sparkTimer += deltaTime
        if (sparkTimer >= SPARK_INTERVAL) {
            sparkTimer = 0f
            val back = rotation + Math.PI.toFloat()
            val a = back + (Random.nextFloat() - 0.5f) * 0.9f
            val speed = 120f + Random.nextFloat() * 180f
            sparks.add(SawSpark(
                x = x + cos(back) * 12f, y = y + sin(back) * 12f,
                vx = cos(a) * speed, vy = sin(a) * speed,
                lifetime = SPARK_LIFETIME, color = EMBER_COLOR
            ))
        }
    }

    fun clear() {
        ghosts.clear()
        sparks.clear()
        ghostTimer = 0f
        sparkTimer = 0f
    }
}

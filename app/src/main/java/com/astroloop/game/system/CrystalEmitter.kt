package com.astroloop.game.system

import kotlin.math.cos
import kotlin.math.sin

/** One crystal bullet to spawn. Carries its own color — there is no pattern tag. */
data class BulletSpec(val x: Float, val y: Float, val vx: Float, val vy: Float, val color: Int)

/**
 * One spiral drip layer. The reckoning is nothing but these: each phase adds one
 * permanently, so P5 runs all five at once. Mixed rotation directions are deliberate —
 * counter-rotating arms interfere and stay readable where uniform noise would not.
 *
 * Density: live = (arms / interval) * (1500 / speed). The sum across active layers must
 * stay well under 200 (projectile pool pre-allocation) — currently ~86 stationary at P5.
 * Interval also sets the picket spacing (speed * interval) a crossing player threads, so
 * it is fairness-tested — see CrystalFightSystemTest.everyStreamIsThreadable.
 */
data class SpiralLayer(
    val color: Int,
    val speed: Float,      // px/s — must beat 375 (max player speed) by >= 1.3x
    val rotSpeed: Float,   // rad/s; sign = direction (+ CW, - CCW)
    val arms: Int,
    val interval: Float    // seconds between emissions
)

/**
 * Pure spiral emitter for the reckoning. One call = one emission from the crystal for
 * ONE layer; CrystalFightSystem owns the rotating angle and schedules the calls.
 * No Random, no state — same arguments, same bullets.
 */
object CrystalEmitter {
    private const val TWO_PI = (2.0 * Math.PI).toFloat()

    fun emit(layer: SpiralLayer, crystalX: Float, crystalY: Float, angle: Float): List<BulletSpec> =
        (0 until layer.arms).map { k ->
            val a = angle + k * (TWO_PI / layer.arms)
            BulletSpec(crystalX, crystalY, cos(a) * layer.speed, sin(a) * layer.speed, layer.color)
        }
}

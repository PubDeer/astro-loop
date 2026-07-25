package com.astroloop.game.render

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.*

object ThrusterDesigns {

    private fun t() = System.currentTimeMillis()

    private fun flicker(freq: Float, phase: Float = 0f): Float {
        val timeS = (t() % 10000L) / 1000f   // 0..10s window, safe float precision
        return 0.7f + 0.3f * sin(timeS * freq + phase)
    }

    private fun pulse(periodMs: Long): Float =
        (t() % periodMs).toFloat() / periodMs.toFloat()

    // 1. Current (reference) — three-line classic
    fun design01_current(canvas: Canvas, cx: Float, cy: Float, size: Float, sr: ShapeRenderer,
                         color: Int = 0xFFFF8800.toInt()) {
        val f1 = flicker(0.020f); val f2 = flicker(0.025f, 1f); val f3 = flicker(0.030f, 2f)
        val len = size * (0.5f + f1 * 1.5f); val sideLen = len * 0.7f; val spread = size * 0.3f
        // core flame
        sr.setColor(color); sr.setAlpha(f1); sr.setStrokeWidth(4f * f1 + 1f)
        sr.drawLine(canvas, cx, cy, cx - len * f1, cy)
        // side wisps
        sr.setColor(0xFFFFAA44.toInt()); sr.setAlpha(f2 * 0.8f); sr.setStrokeWidth(2f * f2 + 0.5f)
        sr.drawLine(canvas, cx, cy, cx - sideLen * f2, cy - spread * f2)
        sr.drawLine(canvas, cx, cy, cx - sideLen * f2, cy + spread * f2)
        // glow haze
        sr.setColor(0xFFFFDD88.toInt()); sr.setAlpha(f3 * 0.4f); sr.setStrokeWidth(6f * f1)
        sr.drawLine(canvas, cx, cy, cx - len * 0.6f * f3, cy)
        sr.setAlpha(1f); sr.setStrokeWidth(1f)
    }

    // 2. Narrow Jet — thin high-velocity beam
    fun design02_narrowJet(canvas: Canvas, cx: Float, cy: Float, size: Float, sr: ShapeRenderer) {
        val f = flicker(0.035f)
        sr.setColor(0xFFCCEEFF.toInt()); sr.setAlpha(0.9f * f); sr.setStrokeWidth(1.5f)
        sr.drawLine(canvas, cx, cy, cx - size * 2.2f * f, cy)
        sr.setColor(0xFFFFFFFF.toInt()); sr.setAlpha(0.6f * f); sr.setStrokeWidth(0.8f)
        sr.drawLine(canvas, cx, cy, cx - size * 2.5f * f, cy)
        sr.setAlpha(1f); sr.setStrokeWidth(1f)
    }

    // 3. Wide Fan — 5 diverging rays
    fun design03_wideFan(canvas: Canvas, cx: Float, cy: Float, size: Float, sr: ShapeRenderer) {
        val f = flicker(0.018f); val len = size * 0.7f * f
        sr.setColor(0xFFFFAA22.toInt()); sr.setAlpha(0.85f); sr.setStrokeWidth(1.5f)
        for (i in -2..2) {
            val angle = i * (PI / 8.0).toFloat()
            sr.drawLine(canvas, cx, cy, cx - len * cos(angle), cy + len * sin(angle))
        }
        sr.setAlpha(1f); sr.setStrokeWidth(1f)
    }

    // 4. Diamond — 5-point star shape
    fun design04_diamond(canvas: Canvas, cx: Float, cy: Float, size: Float, sr: ShapeRenderer) {
        val f = flicker(0.022f); val d = size * 0.9f * f
        sr.setColor(0xFFFF6600.toInt()); sr.setAlpha(0.9f); sr.setStrokeWidth(2.5f)
        sr.drawLine(canvas, cx, cy, cx - d, cy)
        sr.drawLine(canvas, cx, cy, cx, cy - d * 0.5f)
        sr.drawLine(canvas, cx, cy, cx, cy + d * 0.5f)
        sr.drawLine(canvas, cx, cy, cx - d * 0.7f, cy - d * 0.4f)
        sr.drawLine(canvas, cx, cy, cx - d * 0.7f, cy + d * 0.4f)
        sr.setAlpha(1f); sr.setStrokeWidth(1f)
    }

    // 5. Double Cone — inner and outer flame cones
    fun design05_doubleCone(canvas: Canvas, cx: Float, cy: Float, size: Float, sr: ShapeRenderer) {
        val f1 = flicker(0.020f); val f2 = flicker(0.028f, 0.5f)
        val len = size * 1.2f; val s1 = size * 0.35f; val s2 = size * 0.18f
        sr.setColor(0xFFFF8800.toInt()); sr.setAlpha(f1); sr.setStrokeWidth(2f)
        sr.drawLine(canvas, cx, cy, cx - len * f1, cy - s1 * f1)
        sr.drawLine(canvas, cx, cy, cx - len * f1, cy + s1 * f1)
        sr.setColor(0xFFFFDD44.toInt()); sr.setAlpha(f2 * 0.8f); sr.setStrokeWidth(1.5f)
        sr.drawLine(canvas, cx, cy, cx - len * 0.6f * f2, cy - s2 * f2)
        sr.drawLine(canvas, cx, cy, cx - len * 0.6f * f2, cy + s2 * f2)
        sr.setAlpha(1f); sr.setStrokeWidth(1f)
    }

    // 6. Stepped — three tiers of decreasing spread
    fun design06_stepped(canvas: Canvas, cx: Float, cy: Float, size: Float, sr: ShapeRenderer) {
        val f = flicker(0.021f); val step = size * 0.5f
        sr.setColor(0xFFFF9900.toInt()); sr.setAlpha(f); sr.setStrokeWidth(3f)
        sr.drawLine(canvas, cx, cy, cx - step * 2f * f, cy)
        sr.setStrokeWidth(2f)
        sr.drawLine(canvas, cx, cy, cx - step * 1.5f * f, cy - size * 0.25f * f)
        sr.drawLine(canvas, cx, cy, cx - step * 1.5f * f, cy + size * 0.25f * f)
        sr.setStrokeWidth(1f)
        sr.drawLine(canvas, cx, cy, cx - step * f, cy - size * 0.45f * f)
        sr.drawLine(canvas, cx, cy, cx - step * f, cy + size * 0.45f * f)
        sr.setAlpha(1f)
    }

    // 7. Ionic Blue — cold, thin, electric
    fun design07_ionicBlue(canvas: Canvas, cx: Float, cy: Float, size: Float, sr: ShapeRenderer) {
        val f = flicker(0.040f)
        sr.setColor(0xFF4488FF.toInt()); sr.setAlpha(0.95f * f); sr.setStrokeWidth(1f)
        sr.drawLine(canvas, cx, cy, cx - size * 2f * f, cy)
        sr.setColor(0xFFAADDFF.toInt()); sr.setAlpha(0.3f * f); sr.setStrokeWidth(4f)
        sr.drawLine(canvas, cx, cy, cx - size * 1.6f * f, cy)
        sr.setAlpha(1f); sr.setStrokeWidth(1f)
    }

    // 8. Plasma Pulse — expanding ring + core
    fun design08_plasmaPulse(canvas: Canvas, cx: Float, cy: Float, size: Float, sr: ShapeRenderer) {
        val p = pulse(600); val radius = size * (0.2f + p * 0.4f); val alpha = (1f - p) * 0.9f
        sr.setColor(0xFFFF44CC.toInt()); sr.setAlpha(alpha); sr.setStrokeWidth(2f)
        sr.drawCircle(canvas, cx - size * 0.3f, cy, radius)
        sr.setColor(0xFFFF8800.toInt()); sr.setAlpha(0.8f); sr.setStrokeWidth(2f)
        sr.drawLine(canvas, cx, cy, cx - size * 0.5f * (1f - p * 0.3f), cy)
        sr.setAlpha(1f); sr.setStrokeWidth(1f)
    }

    // 9. Afterburner — long hot core with constriction ring
    fun design09_afterburner(canvas: Canvas, cx: Float, cy: Float, size: Float, sr: ShapeRenderer) {
        val f = flicker(0.015f); val len = size * 2.8f * f
        sr.setColor(0xFFFF4400.toInt()); sr.setAlpha(f); sr.setStrokeWidth(2.5f)
        sr.drawLine(canvas, cx, cy, cx - len, cy)
        val ringX = cx - len * 0.35f
        sr.setColor(0xFFFFCC00.toInt()); sr.setAlpha(0.7f * f); sr.setStrokeWidth(1f)
        sr.drawLine(canvas, ringX, cy - size * 0.12f, ringX, cy + size * 0.12f)
        sr.setAlpha(1f); sr.setStrokeWidth(1f)
    }

    // 10. Wave — sinusoidal flame body
    fun design10_wave(canvas: Canvas, cx: Float, cy: Float, size: Float, sr: ShapeRenderer) {
        val timeS = (t() % 10000L) / 1000f
        sr.setColor(0xFFFF8800.toInt()); sr.setAlpha(0.85f); sr.setStrokeWidth(2f)
        val steps = 10; val len = size * 1.5f
        var prevX = cx; var prevY = cy
        for (i in 1..steps) {
            val frac = i.toFloat() / steps
            val nx = cx - len * frac
            val ny = cy + sin((frac * 3f + timeS * 4f).toDouble()).toFloat() * size * 0.25f
            sr.drawLine(canvas, prevX, prevY, nx, ny)
            prevX = nx; prevY = ny
        }
        sr.setAlpha(1f); sr.setStrokeWidth(1f)
    }

    // 11. Split Fork — two parallel streams
    fun design11_splitFork(canvas: Canvas, cx: Float, cy: Float, size: Float, sr: ShapeRenderer) {
        val f = flicker(0.023f); val len = size * 1.4f * f; val gap = size * 0.2f
        sr.setColor(0xFFFFAA00.toInt()); sr.setAlpha(0.9f); sr.setStrokeWidth(2f)
        sr.drawLine(canvas, cx, cy - gap, cx - len, cy - gap)
        sr.drawLine(canvas, cx, cy + gap, cx - len, cy + gap)
        sr.setAlpha(1f); sr.setStrokeWidth(1f)
    }

    // 12. Diffuse Cloud — seeded random scatter
    fun design12_diffuseCloud(canvas: Canvas, cx: Float, cy: Float, size: Float, sr: ShapeRenderer,
                               color: Int = 0xFFFF9933.toInt()) {
        val f = flicker(0.019f)
        sr.setColor(color); sr.setStrokeWidth(1.5f)
        val rng = kotlin.random.Random(t() / 80)
        for (i in 0..8) {
            val dx = -(size * 0.4f + rng.nextFloat() * size * 1.0f)
            val dy = (rng.nextFloat() - 0.5f) * size * 0.8f
            sr.setAlpha((0.3f + rng.nextFloat() * 0.5f) * f)
            sr.drawLine(canvas, cx, cy, cx + dx, cy + dy)
        }
        sr.setAlpha(1f); sr.setStrokeWidth(1f)
    }

    // 13. Comet Tail — tapered fade
    fun design13_cometTail(canvas: Canvas, cx: Float, cy: Float, size: Float, sr: ShapeRenderer) {
        val f = flicker(0.020f); val len = size * 2f; val steps = 6
        for (i in 0 until steps) {
            val t0 = i.toFloat() / steps; val t1 = (i + 1).toFloat() / steps
            val alpha = (1f - t0) * 0.9f * f; val w = (1f - t0) * 3.5f + 0.5f
            sr.setColor(0xFFFF8800.toInt()); sr.setAlpha(alpha); sr.setStrokeWidth(w)
            sr.drawLine(canvas, cx - len * t0, cy, cx - len * t1, cy)
        }
        sr.setAlpha(1f); sr.setStrokeWidth(1f)
    }

    // 14. Arrowhead — forward-pointing V
    fun design14_arrowhead(canvas: Canvas, cx: Float, cy: Float, size: Float, sr: ShapeRenderer) {
        val f = flicker(0.022f); val tip = cx - size * 1.5f * f; val spread = size * 0.5f * f
        sr.setColor(0xFFFF6600.toInt()); sr.setAlpha(0.9f * f); sr.setStrokeWidth(2.5f)
        sr.drawLine(canvas, cx, cy - spread, tip, cy)
        sr.drawLine(canvas, cx, cy + spread, tip, cy)
        sr.drawLine(canvas, cx, cy - spread, cx, cy + spread)
        sr.setAlpha(1f); sr.setStrokeWidth(1f)
    }

    // 15. Star Burst — 6 rays pointing backward
    fun design15_starBurst(canvas: Canvas, cx: Float, cy: Float, size: Float, sr: ShapeRenderer) {
        val f = flicker(0.025f); val len = size * 0.9f * f
        sr.setColor(0xFFFFCC00.toInt()); sr.setAlpha(0.85f); sr.setStrokeWidth(1.5f)
        for (i in 0 until 6) {
            val angle = i * (PI / 3.0) + PI
            sr.drawLine(canvas, cx, cy, (cx + cos(angle) * len).toFloat(), (cy + sin(angle) * len).toFloat())
        }
        sr.setAlpha(1f); sr.setStrokeWidth(1f)
    }

    // 16. Pulse Ring — expanding circle
    fun design16_pulseRing(canvas: Canvas, cx: Float, cy: Float, size: Float, sr: ShapeRenderer) {
        val p = pulse(800); val r = size * 0.2f + p * size * 0.8f; val alpha = (1f - p) * 0.8f
        sr.setColor(0xFFFF8800.toInt()); sr.setAlpha(alpha); sr.setStrokeWidth(2f)
        sr.drawCircle(canvas, cx - size * 0.3f, cy, r)
        sr.setAlpha(1f); sr.setStrokeWidth(1f)
    }

    // 17. Chevron — double V layers
    fun design17_chevron(canvas: Canvas, cx: Float, cy: Float, size: Float, sr: ShapeRenderer) {
        val f = flicker(0.021f); val d = size * 0.9f * f
        sr.setColor(0xFFFF7700.toInt()); sr.setAlpha(0.9f * f); sr.setStrokeWidth(2.5f)
        sr.drawLine(canvas, cx, cy - d * 0.4f, cx - d, cy)
        sr.drawLine(canvas, cx, cy + d * 0.4f, cx - d, cy)
        sr.setColor(0xFFFFAA44.toInt()); sr.setAlpha(0.6f * f); sr.setStrokeWidth(1.5f)
        sr.drawLine(canvas, cx, cy - d * 0.25f, cx - d * 0.7f, cy)
        sr.drawLine(canvas, cx, cy + d * 0.25f, cx - d * 0.7f, cy)
        sr.setAlpha(1f); sr.setStrokeWidth(1f)
    }

    // 18. Spiral — animated corkscrew
    fun design18_spiral(canvas: Canvas, cx: Float, cy: Float, size: Float, sr: ShapeRenderer) {
        val timeS = (t() % 10000L) / 1000f; val f = flicker(0.020f)
        sr.setColor(0xFFFF6600.toInt()); sr.setStrokeWidth(1.5f)
        val steps = 12; val len = size * 1.6f
        var prevX = cx; var prevY = cy
        for (i in 1..steps) {
            val frac = i.toFloat() / steps
            val angle = frac * PI * 1.5 + timeS * 2.0
            val radius = size * 0.3f * frac
            val nx = cx - len * frac; val ny = cy + sin(angle).toFloat() * radius
            sr.setAlpha((1f - frac * 0.6f) * f)
            sr.drawLine(canvas, prevX, prevY, nx, ny)
            prevX = nx; prevY = ny
        }
        sr.setAlpha(1f); sr.setStrokeWidth(1f)
    }

    // 19. Flare — radial burst with glow haze
    fun design19_flare(canvas: Canvas, cx: Float, cy: Float, size: Float, sr: ShapeRenderer) {
        val f = flicker(0.030f)
        sr.setColor(0xFFFFDD44.toInt()); sr.setAlpha(0.5f * f); sr.setStrokeWidth(8f * f)
        sr.drawLine(canvas, cx, cy, cx - size * 0.6f * f, cy)
        sr.setColor(0xFFFF8800.toInt()); sr.setAlpha(0.8f * f); sr.setStrokeWidth(2f)
        for (i in -3..3) {
            val angle = i * (PI / 7.0).toFloat()
            val len = size * (0.4f + (3 - abs(i)) * 0.15f) * f
            sr.drawLine(canvas, cx, cy, cx + (-cos(angle) * len).toFloat(), cy + (sin(angle) * len).toFloat())
        }
        sr.setAlpha(1f); sr.setStrokeWidth(1f)
    }

    // 20. Crystal — prismatic white beam with colored shards
    fun design20_crystal(canvas: Canvas, cx: Float, cy: Float, size: Float, sr: ShapeRenderer) {
        val f = flicker(0.045f); val p = pulse(1200); val len = size * 1.8f
        sr.setColor(0xFFFFFFFF.toInt()); sr.setAlpha(0.95f * f); sr.setStrokeWidth(1f)
        sr.drawLine(canvas, cx, cy, cx - len * f, cy)
        val shardColors = intArrayOf(0xFF88DDFF.toInt(), 0xFFFFAADD.toInt(), 0xFFAAFFCC.toInt())
        val angles = floatArrayOf(-0.4f, 0.4f, -0.2f)
        for (i in 0..2) {
            val a = angles[i].toDouble()
            val sLen = size * (0.8f + i * 0.15f) * (0.7f + p * 0.3f)
            sr.setColor(shardColors[i]); sr.setAlpha(0.5f * f); sr.setStrokeWidth(0.8f)
            sr.drawLine(canvas, cx, cy, (cx + cos(a + PI) * sLen).toFloat(), (cy + sin(a + PI) * sLen).toFloat())
        }
        sr.setAlpha(1f); sr.setStrokeWidth(1f)
    }
}

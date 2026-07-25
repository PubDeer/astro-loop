package com.astroloop.game.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.*

object FlakDesigns {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val ovalRect = RectF()
    private val ovalRect2 = RectF()

    fun render(canvas: Canvas, design: Int, cx: Float, cy: Float, radius: Float, progress: Float) {
        when (design) {
            0  -> renderBasicBurst(canvas, cx, cy, radius, progress)
            1  -> renderDenseBurst(canvas, cx, cy, radius, progress)
            2  -> renderTaperedSpokes(canvas, cx, cy, radius, progress)
            3  -> renderStaggered(canvas, cx, cy, radius, progress)
            4  -> renderDoubleBurst(canvas, cx, cy, radius, progress)
            5  -> renderLongShrapnel(canvas, cx, cy, radius, progress)
            6  -> renderCluster(canvas, cx, cy, radius, progress)
            7  -> renderBasicPuff(canvas, cx, cy, radius, progress)
            8  -> renderHeavySmoke(canvas, cx, cy, radius, progress)
            9  -> renderSparkRing(canvas, cx, cy, radius, progress)
            10 -> renderTrailingParticles(canvas, cx, cy, radius, progress)
            11 -> renderTwoLayerPuff(canvas, cx, cy, radius, progress)
            12 -> renderDissipatingCloud(canvas, cx, cy, radius, progress)
            13 -> renderBasicHybrid(canvas, cx, cy, radius, progress)
            14 -> renderThickRing(canvas, cx, cy, radius, progress)
            15 -> renderDashedRing(canvas, cx, cy, radius, progress)
            16 -> renderDoubleRing(canvas, cx, cy, radius, progress)
            17 -> renderRingDecay(canvas, cx, cy, radius, progress)
            18 -> renderBurstCrown(canvas, cx, cy, radius, progress)
            19 -> renderFullFlak(canvas, cx, cy, radius, progress)
            20 -> renderHeavyFlak1(canvas, cx, cy, radius, progress)
            21 -> renderHeavyFlak2(canvas, cx, cy, radius, progress)
            22 -> renderHeavyFlak3(canvas, cx, cy, radius, progress)
            23 -> renderHeavyFlak4(canvas, cx, cy, radius, progress)
            24 -> renderHeavyFlak5(canvas, cx, cy, radius, progress)
            25 -> renderLightBurst1(canvas, cx, cy, radius, progress)
            26 -> renderLightBurst2(canvas, cx, cy, radius, progress)
            27 -> renderLightBurst3(canvas, cx, cy, radius, progress)
            28 -> renderLightBurst4(canvas, cx, cy, radius, progress)
            29 -> renderLightBurst5(canvas, cx, cy, radius, progress)
            30 -> renderFlashbang1(canvas, cx, cy, radius, progress)
            31 -> renderFlashbang2(canvas, cx, cy, radius, progress)
            32 -> renderFlashbang3(canvas, cx, cy, radius, progress)
            33 -> renderFlashbang4(canvas, cx, cy, radius, progress)
            34 -> renderFlashbang5(canvas, cx, cy, radius, progress)
            35 -> renderEmberCloud1(canvas, cx, cy, radius, progress)
            36 -> renderEmberCloud2(canvas, cx, cy, radius, progress)
            37 -> renderEmberCloud3(canvas, cx, cy, radius, progress)
            38 -> renderEmberCloud4(canvas, cx, cy, radius, progress)
            39 -> renderEmberCloud5(canvas, cx, cy, radius, progress)
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.alpha = 255
        paint.color = 0xFFFFFFFF.toInt()
        fillPaint.alpha = 255
        fillPaint.color = 0xFF000000.toInt()
    }

    // Shrapnel Burst (0–6)

    private fun renderBasicBurst(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = (1f - progress)
        val len = progress * radius
        paint.color = 0xFFFFAA44.toInt()
        paint.strokeWidth = 2f
        paint.alpha = (alpha * 255).toInt()
        for (i in 0 until 8) {
            val angle = i * PI.toFloat() / 4f
            canvas.drawLine(cx, cy, cx + cos(angle) * len, cy + sin(angle) * len, paint)
        }
    }

    private fun renderDenseBurst(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = (1f - progress)
        val len = progress * radius * 0.8f
        paint.color = 0xFFFFCC33.toInt()
        paint.strokeWidth = 1.5f
        paint.alpha = (alpha * 255).toInt()
        for (i in 0 until 16) {
            val jitter = sin(i * 7.3f) * 0.15f
            val angle = i * PI.toFloat() / 8f + jitter
            canvas.drawLine(cx, cy, cx + cos(angle) * len, cy + sin(angle) * len, paint)
        }
    }

    private fun renderTaperedSpokes(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = (1f - progress)
        val len = progress * radius
        paint.color = 0xFFFF8833.toInt()
        for (i in 0 until 12) {
            val angle = i * PI.toFloat() / 6f
            val mx = cx + cos(angle) * len * 0.5f
            val my = cy + sin(angle) * len * 0.5f
            val ex = cx + cos(angle) * len
            val ey = cy + sin(angle) * len
            paint.strokeWidth = 3f
            paint.alpha = (alpha * 255).toInt()
            canvas.drawLine(cx, cy, mx, my, paint)
            paint.strokeWidth = 1f
            paint.alpha = (alpha * 180).toInt()
            canvas.drawLine(mx, my, ex, ey, paint)
        }
    }

    private fun renderStaggered(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = (1f - progress)
        val len = progress * radius
        paint.color = 0xFFFFAA44.toInt()
        paint.strokeWidth = 2f
        paint.alpha = (alpha * 255).toInt()
        for (i in 0 until 8) {
            val angle = i * PI.toFloat() / 4f
            canvas.drawLine(cx, cy, cx + cos(angle) * len, cy + sin(angle) * len, paint)
        }
        val secProgress = ((progress - 0.2f) / 0.8f).coerceIn(0f, 1f)
        val secLen = secProgress * radius * 0.7f
        paint.strokeWidth = 1.5f
        paint.alpha = ((1f - secProgress) * 200).toInt()
        for (i in 0 until 8) {
            val angle = i * PI.toFloat() / 4f + PI.toFloat() / 8f
            canvas.drawLine(cx, cy, cx + cos(angle) * secLen, cy + sin(angle) * secLen, paint)
        }
    }

    private fun renderDoubleBurst(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val p1 = (progress / 0.5f).coerceIn(0f, 1f)
        paint.color = 0xFFFFCC44.toInt()
        paint.strokeWidth = 2f
        paint.alpha = ((1f - p1) * 255).toInt()
        for (i in 0 until 8) {
            val angle = i * PI.toFloat() / 4f
            canvas.drawLine(cx, cy, cx + cos(angle) * p1 * radius, cy + sin(angle) * p1 * radius, paint)
        }
        val p2 = ((progress - 0.4f) / 0.6f).coerceIn(0f, 1f)
        paint.strokeWidth = 1.5f
        paint.alpha = ((1f - p2) * 200).toInt()
        for (i in 0 until 6) {
            val angle = i * PI.toFloat() / 3f + PI.toFloat() / 6f
            canvas.drawLine(cx, cy, cx + cos(angle) * p2 * radius * 0.6f, cy + sin(angle) * p2 * radius * 0.6f, paint)
        }
    }

    private fun renderLongShrapnel(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = (1f - progress)
        val len = progress * radius * 1.1f
        paint.color = 0xFFFF6633.toInt()
        paint.strokeWidth = 2.5f
        paint.alpha = (alpha * 255).toInt()
        val jitters = floatArrayOf(0f, 0.3f, -0.2f, 0.4f, -0.35f, 0.1f)
        for (i in 0 until 6) {
            val angle = i * PI.toFloat() / 3f + jitters[i]
            canvas.drawLine(cx, cy, cx + cos(angle) * len, cy + sin(angle) * len, paint)
        }
    }

    private fun renderCluster(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = (1f - progress)
        val len = progress * radius
        paint.color = 0xFFFFAA33.toInt()
        paint.strokeWidth = 2f
        paint.alpha = (alpha * 255).toInt()
        for (group in 0 until 3) {
            val baseAngle = group * 2f * PI.toFloat() / 3f
            for (j in 0 until 4) {
                val angle = baseAngle + (j - 1.5f) * 0.2f
                canvas.drawLine(cx, cy, cx + cos(angle) * len, cy + sin(angle) * len, paint)
            }
        }
    }

    // Smoke Puff + Fragments (7–12)

    private fun renderBasicPuff(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val puffAlpha = (1f - progress * 1.2f).coerceAtLeast(0f)
        fillPaint.color = 0xFF554433.toInt()
        fillPaint.alpha = (puffAlpha * 180).toInt()
        canvas.drawCircle(cx, cy, progress * radius * 0.6f, fillPaint)
        paint.color = 0xFFFF8833.toInt()
        paint.strokeWidth = 4f
        paint.alpha = ((1f - progress) * 255).toInt()
        val pDist = progress * radius
        for (i in 0 until 8) {
            val angle = i * PI.toFloat() / 4f
            canvas.drawPoint(cx + cos(angle) * pDist, cy + sin(angle) * pDist, paint)
        }
    }

    private fun renderHeavySmoke(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val puffAlpha = (1f - progress * 0.9f).coerceAtLeast(0f)
        fillPaint.color = 0xFF443322.toInt()
        fillPaint.alpha = (puffAlpha * 220).toInt()
        canvas.drawCircle(cx, cy, (progress * 0.7f + 0.1f) * radius, fillPaint)
        paint.color = 0xFFFFAA44.toInt()
        paint.strokeWidth = 4f
        paint.alpha = ((1f - progress) * 255).toInt()
        for (i in 0 until 12) {
            val drift = sin(i * 3.7f) * 0.4f
            val angle = i * PI.toFloat() / 6f + drift * progress
            val pDist = progress * radius * (0.7f + sin(i * 5.2f) * 0.3f)
            canvas.drawPoint(cx + cos(angle) * pDist, cy + sin(angle) * pDist, paint)
        }
    }

    private fun renderSparkRing(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val puffAlpha = (1f - progress * 1.5f).coerceAtLeast(0f)
        fillPaint.color = 0xFF333333.toInt()
        fillPaint.alpha = (puffAlpha * 200).toInt()
        canvas.drawCircle(cx, cy, progress * radius * 0.5f, fillPaint)
        paint.color = 0xFFFFEE44.toInt()
        paint.strokeWidth = 4f
        paint.alpha = ((1f - progress) * 255).toInt()
        val pDist = progress * radius
        for (i in 0 until 12) {
            val angle = i * PI.toFloat() / 6f
            canvas.drawPoint(cx + cos(angle) * pDist, cy + sin(angle) * pDist, paint)
        }
    }

    private fun renderTrailingParticles(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        // Small smoke puff at center
        val puffAlpha = (1f - progress * 1.5f).coerceAtLeast(0f)
        fillPaint.color = 0xFF443322.toInt()
        fillPaint.alpha = (puffAlpha * 160).toInt()
        canvas.drawCircle(cx, cy, progress * radius * 0.35f, fillPaint)
        val alpha = (1f - progress)
        paint.color = 0xFFFF8844.toInt()
        paint.strokeWidth = 2f
        paint.alpha = (alpha * 255).toInt()
        for (i in 0 until 10) {
            val angle = i * 2f * PI.toFloat() / 10f
            val dist = progress * radius
            val trailDist = (progress - 0.15f).coerceAtLeast(0f) * radius
            canvas.drawLine(
                cx + cos(angle) * trailDist, cy + sin(angle) * trailDist,
                cx + cos(angle) * dist, cy + sin(angle) * dist, paint
            )
            paint.strokeWidth = 3f
            canvas.drawPoint(cx + cos(angle) * dist, cy + sin(angle) * dist, paint)
            paint.strokeWidth = 2f
        }
    }

    private fun renderTwoLayerPuff(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val innerP = (progress * 1.5f).coerceAtMost(1f)
        fillPaint.color = 0xFF554422.toInt()
        fillPaint.alpha = ((1f - innerP).coerceAtLeast(0f) * 220).toInt()
        canvas.drawCircle(cx, cy, innerP * radius * 0.5f, fillPaint)
        paint.color = 0xFFFFAA44.toInt()
        paint.strokeWidth = 3f
        paint.alpha = ((1f - progress) * 255).toInt()
        val outerDist = progress * radius
        for (i in 0 until 8) {
            val angle = i * PI.toFloat() / 4f
            canvas.drawPoint(cx + cos(angle) * outerDist, cy + sin(angle) * outerDist, paint)
        }
        paint.strokeWidth = 2f
        paint.alpha = ((1f - progress) * 180).toInt()
        val innerDist = progress * radius * 0.6f
        for (i in 0 until 8) {
            val angle = i * PI.toFloat() / 4f + PI.toFloat() / 8f
            canvas.drawPoint(cx + cos(angle) * innerDist, cy + sin(angle) * innerDist, paint)
        }
    }

    private fun renderDissipatingCloud(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val puffAlpha = (1f - progress * 1.3f).coerceAtLeast(0f)
        fillPaint.color = 0xFF444444.toInt()
        fillPaint.alpha = (puffAlpha * 200).toInt()
        canvas.drawCircle(cx, cy, progress * radius * 0.4f, fillPaint)
        val dotProgress = (progress * 1.2f - 0.2f).coerceIn(0f, 1f)
        paint.color = 0xFF998877.toInt()
        paint.strokeWidth = 5f
        paint.alpha = ((1f - dotProgress) * 200).toInt()
        for (i in 0 until 6) {
            val angle = i * PI.toFloat() / 3f + progress * 0.3f
            val dist = dotProgress * radius * 0.8f
            canvas.drawPoint(cx + cos(angle) * dist, cy + sin(angle) * dist, paint)
        }
    }

    // Hybrid Ring + Shrapnel (13–19)

    private fun renderBasicHybrid(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = (1f - progress)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = 0xFFFFAA44.toInt()
        paint.alpha = (alpha * 200).toInt()
        canvas.drawCircle(cx, cy, progress * radius, paint)
        paint.strokeWidth = 1.5f
        paint.alpha = (alpha * 255).toInt()
        for (i in 0 until 8) {
            val angle = i * PI.toFloat() / 4f
            val s = progress * radius * 0.8f
            val e = progress * radius * 1.3f
            canvas.drawLine(cx + cos(angle) * s, cy + sin(angle) * s, cx + cos(angle) * e, cy + sin(angle) * e, paint)
        }
    }

    private fun renderThickRing(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = (1f - progress)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        paint.color = 0xFFFF8833.toInt()
        paint.alpha = (alpha * 220).toInt()
        canvas.drawCircle(cx, cy, progress * radius, paint)
        paint.strokeWidth = 3f
        paint.alpha = (alpha * 255).toInt()
        for (i in 0 until 6) {
            val angle = i * PI.toFloat() / 3f
            canvas.drawLine(cx, cy, cx + cos(angle) * progress * radius * 1.4f, cy + sin(angle) * progress * radius * 1.4f, paint)
        }
    }

    private fun renderDashedRing(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = (1f - progress)
        val r = progress * radius
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = 0xFFFFCC44.toInt()
        paint.alpha = (alpha * 220).toInt()
        ovalRect.set(cx - r, cy - r, cx + r, cy + r)
        for (i in 0 until 12) {
            canvas.drawArc(ovalRect, i * 30f - 10f, 20f, false, paint)
        }
        paint.strokeWidth = 1.5f
        paint.alpha = (alpha * 200).toInt()
        for (i in 0 until 12) {
            val angle = (i * 30f + 15f) * PI.toFloat() / 180f
            canvas.drawLine(cx, cy, cx + cos(angle) * r * 1.3f, cy + sin(angle) * r * 1.3f, paint)
        }
    }

    private fun renderDoubleRing(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = (1f - progress)
        paint.style = Paint.Style.STROKE
        val innerR = (progress * 1.2f).coerceAtMost(1f) * radius
        paint.strokeWidth = 2f
        paint.color = 0xFFFFFFAA.toInt()
        paint.alpha = ((1f - progress * 1.2f).coerceAtLeast(0f) * 230).toInt()
        canvas.drawCircle(cx, cy, innerR, paint)
        paint.strokeWidth = 3f
        paint.color = 0xFFFFAA44.toInt()
        paint.alpha = (alpha * 200).toInt()
        canvas.drawCircle(cx, cy, progress * radius * 0.8f, paint)
        paint.strokeWidth = 1.5f
        paint.alpha = (alpha * 220).toInt()
        for (i in 0 until 8) {
            val angle = i * PI.toFloat() / 4f
            canvas.drawLine(cx, cy, cx + cos(angle) * progress * radius, cy + sin(angle) * progress * radius, paint)
        }
    }

    private fun renderRingDecay(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val ringP = (progress / 0.4f).coerceAtMost(1f)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = 0xFFFFAA44.toInt()
        paint.alpha = ((1f - ringP).coerceAtLeast(0f) * 230).toInt()
        canvas.drawCircle(cx, cy, ringP * radius, paint)
        paint.strokeWidth = 2f
        paint.alpha = ((1f - progress) * 255).toInt()
        for (i in 0 until 8) {
            val angle = i * PI.toFloat() / 4f
            canvas.drawLine(cx, cy, cx + cos(angle) * progress * radius * 1.1f, cy + sin(angle) * progress * radius * 1.1f, paint)
        }
    }

    private fun renderBurstCrown(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val r = progress * radius
        val alpha = (1f - progress)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = 0xFFFFEE44.toInt()
        paint.alpha = (alpha * 200).toInt()
        canvas.drawCircle(cx, cy, r, paint)
        paint.strokeWidth = 2f
        paint.alpha = (alpha * 255).toInt()
        for (i in 0 until 16) {
            val angle = i * PI.toFloat() / 8f
            canvas.drawLine(
                cx + cos(angle) * r, cy + sin(angle) * r,
                cx + cos(angle) * (r + radius * 0.2f), cy + sin(angle) * (r + radius * 0.2f), paint
            )
        }
    }

    private fun renderFullFlak(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = (1f - progress)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = 0xFFFF8833.toInt()
        paint.alpha = (alpha * 200).toInt()
        canvas.drawCircle(cx, cy, progress * radius, paint)
        val innerR = (progress * 1.5f).coerceAtMost(1f) * radius * 0.5f
        paint.strokeWidth = 2f
        paint.color = 0xFFFFCC44.toInt()
        paint.alpha = ((1f - progress * 1.5f).coerceAtLeast(0f) * 230).toInt()
        canvas.drawCircle(cx, cy, innerR, paint)
        paint.strokeWidth = 1.5f
        paint.color = 0xFFFF8833.toInt()
        paint.alpha = (alpha * 230).toInt()
        for (i in 0 until 12) {
            val angle = i * PI.toFloat() / 6f
            canvas.drawLine(cx, cy, cx + cos(angle) * progress * radius * 1.2f, cy + sin(angle) * progress * radius * 1.2f, paint)
        }
        paint.strokeWidth = 4f
        paint.color = 0xFFFFFF88.toInt()
        paint.alpha = (alpha * 255).toInt()
        for (i in 0 until 6) {
            val angle = i * PI.toFloat() / 3f + progress * 0.5f
            canvas.drawPoint(cx + cos(angle) * progress * radius * 1.1f, cy + sin(angle) * progress * radius * 1.1f, paint)
        }
    }


    // ── Family 1: Heavy Flak (designs 20–24) ──────────────────────────
    // Dark charcoal smoke, near-opaque. Dense fine shrapnel, deep reds/ambers.

    // Design 20 — 16 deep-red lines, dark charcoal smoke
    private fun renderHeavyFlak1(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = 1f - progress
        fillPaint.color = 0xFF333333.toInt()
        fillPaint.alpha = (alpha * 210).toInt()
        canvas.drawCircle(cx, cy, radius, fillPaint)
        paint.color = 0xFFCC2200.toInt()
        paint.strokeWidth = 1.5f
        paint.alpha = (alpha * 255).toInt()
        val tipR = progress * radius * 1.12f
        for (i in 0 until 16) {
            val angle = i * PI.toFloat() / 8f + sin(i * 2.7f) * 0.12f
            canvas.drawLine(cx, cy, cx + cos(angle) * tipR, cy + sin(angle) * tipR, paint)
        }
    }

    // Design 21 — 14 blood-orange lines, dark gray smoke
    private fun renderHeavyFlak2(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = 1f - progress
        fillPaint.color = 0xFF444444.toInt()
        fillPaint.alpha = (alpha * 200).toInt()
        canvas.drawCircle(cx, cy, radius, fillPaint)
        paint.color = 0xFFDD4400.toInt()
        paint.strokeWidth = 1.5f
        paint.alpha = (alpha * 255).toInt()
        val tipR = progress * radius * 1.12f
        for (i in 0 until 14) {
            val angle = i * 2f * PI.toFloat() / 14f + sin(i * 3.1f) * 0.1f
            canvas.drawLine(cx, cy, cx + cos(angle) * tipR, cy + sin(angle) * tipR, paint)
        }
    }

    // Design 22 — 16 crimson lines, charcoal smoke
    private fun renderHeavyFlak3(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = 1f - progress
        fillPaint.color = 0xFF3A3A3A.toInt()
        fillPaint.alpha = (alpha * 215).toInt()
        canvas.drawCircle(cx, cy, radius, fillPaint)
        paint.color = 0xFFBB1100.toInt()
        paint.strokeWidth = 2f
        paint.alpha = (alpha * 255).toInt()
        val tipR = progress * radius * 1.15f
        for (i in 0 until 16) {
            val angle = i * PI.toFloat() / 8f
            canvas.drawLine(cx, cy, cx + cos(angle) * tipR, cy + sin(angle) * tipR, paint)
        }
    }

    // Design 23 — 12 brick-red lines, dark slate smoke
    private fun renderHeavyFlak4(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = 1f - progress
        fillPaint.color = 0xFF3D3D3D.toInt()
        fillPaint.alpha = (alpha * 200).toInt()
        canvas.drawCircle(cx, cy, radius, fillPaint)
        paint.color = 0xFFCC3300.toInt()
        paint.strokeWidth = 2f
        paint.alpha = (alpha * 255).toInt()
        val tipR = progress * radius * 1.1f
        for (i in 0 until 12) {
            val angle = i * PI.toFloat() / 6f + sin(i * 4.3f) * 0.15f
            canvas.drawLine(cx, cy, cx + cos(angle) * tipR, cy + sin(angle) * tipR, paint)
        }
    }

    // Design 24 — 14 dark-amber lines, gunmetal smoke
    private fun renderHeavyFlak5(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = 1f - progress
        fillPaint.color = 0xFF404040.toInt()
        fillPaint.alpha = (alpha * 210).toInt()
        canvas.drawCircle(cx, cy, radius, fillPaint)
        paint.color = 0xFFCC5500.toInt()
        paint.strokeWidth = 1.5f
        paint.alpha = (alpha * 255).toInt()
        val tipR = progress * radius * 1.12f
        for (i in 0 until 14) {
            val angle = i * 2f * PI.toFloat() / 14f
            canvas.drawLine(cx, cy, cx + cos(angle) * tipR, cy + sin(angle) * tipR, paint)
        }
    }

    // ── Family 2: Light Burst (designs 25–29) ─────────────────────────
    // Light gray wispy smoke, low opacity. Fewer thick orange shrapnel chunks.

    // Design 25 — 8 orange chunks, light gray smoke
    private fun renderLightBurst1(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = 1f - progress
        fillPaint.color = 0xFF888888.toInt()
        fillPaint.alpha = (alpha * 100).toInt()
        canvas.drawCircle(cx, cy, radius, fillPaint)
        paint.color = 0xFFFF8800.toInt()
        paint.strokeWidth = 3f
        paint.alpha = (alpha * 255).toInt()
        val tipR = progress * radius * 1.15f
        for (i in 0 until 8) {
            val angle = i * PI.toFloat() / 4f + sin(i * 5.1f) * 0.2f
            canvas.drawLine(cx, cy, cx + cos(angle) * tipR, cy + sin(angle) * tipR, paint)
        }
    }

    // Design 26 — 6 bright-orange chunks + thin ring, pale smoke
    private fun renderLightBurst2(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = 1f - progress
        fillPaint.color = 0xFF999999.toInt()
        fillPaint.alpha = (alpha * 90).toInt()
        canvas.drawCircle(cx, cy, radius, fillPaint)
        paint.color = 0xFFFF9922.toInt()
        paint.strokeWidth = 1.5f
        paint.alpha = (alpha * 180).toInt()
        canvas.drawCircle(cx, cy, radius, paint)
        paint.strokeWidth = 4f
        paint.alpha = (alpha * 255).toInt()
        val tipR = progress * radius * 1.15f
        for (i in 0 until 6) {
            val angle = i * PI.toFloat() / 3f
            canvas.drawLine(cx, cy, cx + cos(angle) * tipR, cy + sin(angle) * tipR, paint)
        }
    }

    // Design 27 — 8 orange-red chunks, light gray smoke
    private fun renderLightBurst3(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = 1f - progress
        fillPaint.color = 0xFF7A7A7A.toInt()
        fillPaint.alpha = (alpha * 100).toInt()
        canvas.drawCircle(cx, cy, radius, fillPaint)
        paint.color = 0xFFFF6600.toInt()
        paint.strokeWidth = 3.5f
        paint.alpha = (alpha * 255).toInt()
        val tipR = progress * radius * 1.12f
        for (i in 0 until 8) {
            val angle = i * PI.toFloat() / 4f + sin(i * 3.7f) * 0.18f
            canvas.drawLine(cx, cy, cx + cos(angle) * tipR, cy + sin(angle) * tipR, paint)
        }
    }

    // Design 28 — 6 warm-orange chunks + thin ring, pale smoke
    private fun renderLightBurst4(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = 1f - progress
        fillPaint.color = 0xFF909090.toInt()
        fillPaint.alpha = (alpha * 80).toInt()
        canvas.drawCircle(cx, cy, radius, fillPaint)
        paint.color = 0xFFFFAA33.toInt()
        paint.strokeWidth = 1.5f
        paint.alpha = (alpha * 160).toInt()
        canvas.drawCircle(cx, cy, radius, paint)
        paint.strokeWidth = 4f
        paint.alpha = (alpha * 255).toInt()
        val tipR = progress * radius * 1.15f
        for (i in 0 until 6) {
            val angle = i * PI.toFloat() / 3f + sin(i * 2.9f) * 0.15f
            canvas.drawLine(cx, cy, cx + cos(angle) * tipR, cy + sin(angle) * tipR, paint)
        }
    }

    // Design 29 — 8 orange chunks, wispy pale smoke
    private fun renderLightBurst5(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = 1f - progress
        fillPaint.color = 0xFF858585.toInt()
        fillPaint.alpha = (alpha * 95).toInt()
        canvas.drawCircle(cx, cy, radius, fillPaint)
        paint.color = 0xFFFF7700.toInt()
        paint.strokeWidth = 3f
        paint.alpha = (alpha * 255).toInt()
        val tipR = progress * radius * 1.1f
        for (i in 0 until 8) {
            val angle = i * PI.toFloat() / 4f
            canvas.drawLine(cx, cy, cx + cos(angle) * tipR, cy + sin(angle) * tipR, paint)
        }
    }

    // ── Family 3: Flashbang (designs 30–34) ───────────────────────────
    // Faint gray wash. Dense white-hot/yellow lines. Some with brief core flash.

    // Design 30 — 20 white-hot lines, faint gray wash
    private fun renderFlashbang1(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = 1f - progress
        fillPaint.color = 0xFF888888.toInt()
        fillPaint.alpha = (alpha * 40).toInt()
        canvas.drawCircle(cx, cy, radius, fillPaint)
        paint.color = 0xFFFFFFFF.toInt()
        paint.strokeWidth = 1f
        paint.alpha = (alpha * 255).toInt()
        val tipR = progress * radius * 1.15f
        for (i in 0 until 20) {
            val angle = i * PI.toFloat() / 10f + sin(i * 1.9f) * 0.08f
            canvas.drawLine(cx, cy, cx + cos(angle) * tipR, cy + sin(angle) * tipR, paint)
        }
    }

    // Design 31 — 18 yellow-white lines + brief core flash
    private fun renderFlashbang2(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = 1f - progress
        fillPaint.color = 0xFF888888.toInt()
        fillPaint.alpha = (alpha * 35).toInt()
        canvas.drawCircle(cx, cy, radius, fillPaint)
        val coreAlpha = ((1f - progress * 3f).coerceAtLeast(0f) * 220).toInt()
        fillPaint.color = 0xFFFFFFCC.toInt()
        fillPaint.alpha = coreAlpha
        canvas.drawCircle(cx, cy, radius * 0.25f, fillPaint)
        paint.color = 0xFFFFFF88.toInt()
        paint.strokeWidth = 1.5f
        paint.alpha = (alpha * 255).toInt()
        val tipR = progress * radius * 1.12f
        for (i in 0 until 18) {
            val angle = i * 2f * PI.toFloat() / 18f
            canvas.drawLine(cx, cy, cx + cos(angle) * tipR, cy + sin(angle) * tipR, paint)
        }
    }

    // Design 32 — 16 pure-yellow lines, near-transparent smoke
    private fun renderFlashbang3(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = 1f - progress
        fillPaint.color = 0xFF999999.toInt()
        fillPaint.alpha = (alpha * 30).toInt()
        canvas.drawCircle(cx, cy, radius, fillPaint)
        paint.color = 0xFFFFEE00.toInt()
        paint.strokeWidth = 1.5f
        paint.alpha = (alpha * 255).toInt()
        val tipR = progress * radius * 1.15f
        for (i in 0 until 16) {
            val angle = i * PI.toFloat() / 8f + sin(i * 3.3f) * 0.1f
            canvas.drawLine(cx, cy, cx + cos(angle) * tipR, cy + sin(angle) * tipR, paint)
        }
    }

    // Design 33 — 20 warm-white lines + brief core flash
    private fun renderFlashbang4(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = 1f - progress
        fillPaint.color = 0xFF777777.toInt()
        fillPaint.alpha = (alpha * 40).toInt()
        canvas.drawCircle(cx, cy, radius, fillPaint)
        val coreAlpha = ((1f - progress * 3f).coerceAtLeast(0f) * 230).toInt()
        fillPaint.color = 0xFFFFEECC.toInt()
        fillPaint.alpha = coreAlpha
        canvas.drawCircle(cx, cy, radius * 0.3f, fillPaint)
        paint.color = 0xFFFFEECC.toInt()
        paint.strokeWidth = 1f
        paint.alpha = (alpha * 255).toInt()
        val tipR = progress * radius * 1.12f
        for (i in 0 until 20) {
            val angle = i * PI.toFloat() / 10f
            canvas.drawLine(cx, cy, cx + cos(angle) * tipR, cy + sin(angle) * tipR, paint)
        }
    }

    // Design 34 — 18 yellow-orange lines, very faint smoke
    private fun renderFlashbang5(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = 1f - progress
        fillPaint.color = 0xFF888888.toInt()
        fillPaint.alpha = (alpha * 25).toInt()
        canvas.drawCircle(cx, cy, radius, fillPaint)
        paint.color = 0xFFFFCC44.toInt()
        paint.strokeWidth = 1.5f
        paint.alpha = (alpha * 255).toInt()
        val tipR = progress * radius * 1.15f
        for (i in 0 until 18) {
            val angle = i * 2f * PI.toFloat() / 18f + sin(i * 2.1f) * 0.1f
            canvas.drawLine(cx, cy, cx + cos(angle) * tipR, cy + sin(angle) * tipR, paint)
        }
    }

    // ── Family 4: Ember Cloud (designs 35–39) ─────────────────────────
    // Medium gray smoke. Ember dots expand from center outward. Some add outer ring.

    // Draws a radial-gradient smoke circle: full alpha at center, 60% alpha at edge.
    private fun drawGradientSmoke(canvas: Canvas, cx: Float, cy: Float, radius: Float, progressAlpha: Float, rgb: Int, maxAlpha: Int) {
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        val centerA = (progressAlpha * maxAlpha).toInt().coerceIn(0, 255)
        val edgeA = (progressAlpha * maxAlpha * 0.6f).toInt().coerceIn(0, 255)
        val centerColor = (centerA shl 24) or (r shl 16) or (g shl 8) or b
        val edgeColor = (edgeA shl 24) or (r shl 16) or (g shl 8) or b
        fillPaint.shader = RadialGradient(cx, cy, radius, centerColor, edgeColor, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, radius, fillPaint)
        fillPaint.shader = null
    }

    // Design 35 — 14 orange ember dots, gradient smoke
    private fun renderEmberCloud1(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = 1f - progress
        drawGradientSmoke(canvas, cx, cy, radius, alpha, 0x555555, 160)
        paint.color = 0xFFFF6600.toInt()
        paint.strokeWidth = 6f
        paint.alpha = (alpha * 255).toInt()
        for (i in 0 until 14) {
            val angle = i * 2f * PI.toFloat() / 14f + sin(i * 3.7f) * 0.2f
            val dist = progress * radius * (0.9f + sin(i * 5.1f) * 0.25f)
            canvas.drawPoint(cx + cos(angle) * dist, cy + sin(angle) * dist, paint)
        }
    }

    // Design 36 — 12 red-orange ember dots, gradient smoke (no ring)
    private fun renderEmberCloud2(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = 1f - progress
        drawGradientSmoke(canvas, cx, cy, radius, alpha, 0x4A4A4A, 165)
        paint.color = 0xFFFF4400.toInt()
        paint.strokeWidth = 6f
        paint.alpha = (alpha * 255).toInt()
        for (i in 0 until 12) {
            val angle = i * PI.toFloat() / 6f + sin(i * 4.3f) * 0.2f
            val dist = progress * radius * (0.9f + sin(i * 6.3f) * 0.2f)
            canvas.drawPoint(cx + cos(angle) * dist, cy + sin(angle) * dist, paint)
        }
    }

    // Design 37 — 16 orange ember dots, medium gray smoke
    private fun renderEmberCloud3(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = 1f - progress
        fillPaint.color = 0xFF565656.toInt()
        fillPaint.alpha = (alpha * 155).toInt()
        canvas.drawCircle(cx, cy, radius, fillPaint)
        paint.color = 0xFFFF7700.toInt()
        paint.strokeWidth = 5f
        paint.alpha = (alpha * 255).toInt()
        for (i in 0 until 16) {
            val angle = i * PI.toFloat() / 8f
            val dist = progress * radius * (0.85f + sin(i * 7.1f) * 0.3f)
            canvas.drawPoint(cx + cos(angle) * dist, cy + sin(angle) * dist, paint)
        }
    }

    // Design 38 — 12 deep-orange ember dots, gradient smoke (no ring)
    private fun renderEmberCloud4(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = 1f - progress
        drawGradientSmoke(canvas, cx, cy, radius, alpha, 0x505050, 160)
        paint.color = 0xFFFF5500.toInt()
        paint.strokeWidth = 7f
        paint.alpha = (alpha * 255).toInt()
        for (i in 0 until 12) {
            val angle = i * PI.toFloat() / 6f + sin(i * 2.9f) * 0.25f
            val dist = progress * radius * (0.95f + sin(i * 4.7f) * 0.2f)
            canvas.drawPoint(cx + cos(angle) * dist, cy + sin(angle) * dist, paint)
        }
    }

    // Design 39 — 14 orange + 8 red ember dots (two rings), medium-dark smoke
    private fun renderEmberCloud5(canvas: Canvas, cx: Float, cy: Float, radius: Float, progress: Float) {
        val alpha = 1f - progress
        fillPaint.color = 0xFF4D4D4D.toInt()
        fillPaint.alpha = (alpha * 165).toInt()
        canvas.drawCircle(cx, cy, radius, fillPaint)
        paint.color = 0xFFFF6600.toInt()
        paint.strokeWidth = 6f
        paint.alpha = (alpha * 255).toInt()
        for (i in 0 until 14) {
            val angle = i * 2f * PI.toFloat() / 14f
            val dist = progress * radius * (0.9f + sin(i * 5.3f) * 0.15f)
            canvas.drawPoint(cx + cos(angle) * dist, cy + sin(angle) * dist, paint)
        }
        paint.color = 0xFFFF3300.toInt()
        paint.strokeWidth = 4f
        paint.alpha = (alpha * 220).toInt()
        for (i in 0 until 8) {
            val angle = i * PI.toFloat() / 4f + PI.toFloat() / 14f
            val dist = progress * radius * 0.5f
            canvas.drawPoint(cx + cos(angle) * dist, cy + sin(angle) * dist, paint)
        }
    }
}

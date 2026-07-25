package com.astroloop.game.render

import android.graphics.Canvas
import android.graphics.Paint
import com.astroloop.game.core.GameState
import kotlin.math.sin
import kotlin.random.Random

data class CrackSegment(
    val x1: Float, val y1: Float,
    val x2: Float, val y2: Float,
    val delay: Float  // 0.0-1.0: when this segment starts appearing relative to coverage
)

class CrystalRenderer {

    companion object {
        const val DEATH_COVERAGE_RATE = 0.67f
        const val DEATH_TEXT_THRESHOLD = 0.6f
        const val DEATH_TEXT_FADE_RATE = 2.0f
        const val DEATH_HOLD_DURATION = 1.0f

        const val PAUSE_COVERAGE_RATE = 0.65f
        const val PAUSE_COVERAGE_MAX = 0.45f
        const val PAUSE_TEXT_THRESHOLD = 0.25f
        const val PAUSE_DISSOLVE_RATE = 3.0f

        const val CRACK_GROW_DURATION = 0.2f
        const val GRID_COLS = 8
        const val GRID_ROWS = 14
        const val GRID_JITTER = 0.7f
    }

    // Localized crystal zap state
    private var zapSegments: List<CrackSegment> = emptyList()
    private var zapCoverage: Float = 0f
    private var zapAlpha: Float = 1f
    private var zapFlashAlpha: Float = 1f
    var zapActive: Boolean = false
        private set

    private var segments: List<CrackSegment> = emptyList()
    private var coverage: Float = 0f
    private var targetCoverage: Float = 0f
    private var textAlpha: Float = 0f
    private var holdTimer: Float = 0f
    private var animationTime: Float = 0f

    var isActive: Boolean = false
        private set
    var isPause: Boolean = false
        private set
    var isDissolving: Boolean = false
        private set
    var isComplete: Boolean = false
        private set

    private val glowPaint = Paint().apply {
        color = 0xFF44AACC.toInt()
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val corePaint = Paint().apply {
        color = 0xFFCCEEFF.toInt()
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 48f
        textAlign = Paint.Align.CENTER
        typeface = FontManager.getDisplayRegular()
        isAntiAlias = true
    }

    private val textGlowPaint = Paint().apply {
        color = 0xFF88EEFF.toInt()
        textSize = 48f
        textAlign = Paint.Align.CENTER
        typeface = FontManager.getDisplayRegular()
        isAntiAlias = true
    }

    private val flashPaint = Paint().apply {
        color = 0xFFCCEEFF.toInt()  // Icy blue-white — less harsh than pure white in dark rooms
        style = Paint.Style.FILL
    }

    private val orbPaint = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun activateDeath(screenWidth: Float, screenHeight: Float) {
        resetState()
        targetCoverage = 1.0f
        isPause = false
        isActive = true
        segments = generateCrackPattern(screenWidth, screenHeight)
    }

    fun activatePause(screenWidth: Float, screenHeight: Float) {
        resetState()
        targetCoverage = PAUSE_COVERAGE_MAX
        isPause = true
        isActive = true
        segments = generateCrackPattern(screenWidth, screenHeight)
    }

    fun dissolve() {
        if (!isPause) return
        isDissolving = true
    }

    fun deactivate() {
        isActive = false
        segments = emptyList()
    }

    fun update(deltaTime: Float) {
        if (!isActive) return

        animationTime += deltaTime

        if (isDissolving) {
            coverage -= PAUSE_DISSOLVE_RATE * deltaTime
            textAlpha = (coverage / targetCoverage).coerceIn(0f, 1f)
            if (coverage <= 0f) {
                coverage = 0f
                isActive = false
            }
            return
        }

        // Grow coverage toward target
        val rate = if (isPause) PAUSE_COVERAGE_RATE else DEATH_COVERAGE_RATE
        coverage = (coverage + rate * deltaTime).coerceAtMost(targetCoverage)

        // Text fade-in
        val textThreshold = if (isPause) PAUSE_TEXT_THRESHOLD else DEATH_TEXT_THRESHOLD
        if (coverage > textThreshold) {
            textAlpha = (textAlpha + DEATH_TEXT_FADE_RATE * deltaTime).coerceAtMost(1f)
        }

        // Death-only: hold then cut
        if (!isPause && coverage >= targetCoverage && textAlpha >= 1f) {
            holdTimer += deltaTime
            if (holdTimer >= DEATH_HOLD_DURATION) {
                isComplete = true
            }
        }
    }

    fun render(canvas: Canvas, screenWidth: Float, screenHeight: Float) {
        if (!isActive) return

        for (seg in segments) {
            val segProgress = ((coverage - seg.delay * targetCoverage) /
                    (CRACK_GROW_DURATION * targetCoverage)).coerceIn(0f, 1f)
            if (segProgress <= 0f) continue

            val midX = (seg.x1 + seg.x2) / 2f
            val midY = (seg.y1 + seg.y2) / 2f
            val drawX1 = midX + (seg.x1 - midX) * segProgress
            val drawY1 = midY + (seg.y1 - midY) * segProgress
            val drawX2 = midX + (seg.x2 - midX) * segProgress
            val drawY2 = midY + (seg.y2 - midY) * segProgress

            // Shimmer throughout pause expansion and hold (not during dissolve)
            var alpha = 255
            if (isPause && !isDissolving) {
                val shimmer = 0.6f + 0.4f * sin(animationTime * 3f + seg.delay * 6.2832f)
                alpha = (shimmer * 255).toInt().coerceIn(0, 255)
            }

            // Glow pass (thick, transparent)
            glowPaint.alpha = (alpha * 0.5f).toInt()
            canvas.drawLine(drawX1, drawY1, drawX2, drawY2, glowPaint)

            // Core pass (thin, brighter)
            corePaint.alpha = alpha
            canvas.drawLine(drawX1, drawY1, drawX2, drawY2, corePaint)
        }

        // Draw text — pause only. Death crystals stay wordless: the crystalline
        // freeze itself is the Time Crystal activating, hidden in plain sight.
        if (isPause && textAlpha > 0f) {
            val text = "PAUSED"
            val textX = screenWidth / 2f
            val textY = screenHeight * 0.25f

            val alphaInt = (textAlpha * 255).toInt().coerceIn(0, 255)

            // Glow layer underneath
            textGlowPaint.alpha = (alphaInt * 0.6f).toInt()
            canvas.drawText(text, textX, textY - 2f, textGlowPaint)
            canvas.drawText(text, textX, textY + 2f, textGlowPaint)
            canvas.drawText(text, textX - 2f, textY, textGlowPaint)
            canvas.drawText(text, textX + 2f, textY, textGlowPaint)

            // Core text
            textPaint.alpha = alphaInt
            canvas.drawText(text, textX, textY, textPaint)
        }

    }

    private fun resetState() {
        segments = emptyList()
        coverage = 0f
        targetCoverage = 0f
        textAlpha = 0f
        holdTimer = 0f
        animationTime = 0f
        isActive = false
        isPause = false
        isDissolving = false
        isComplete = false
    }

    private fun generateCrackPattern(screenWidth: Float, screenHeight: Float): List<CrackSegment> {
        val cellW = screenWidth / GRID_COLS
        val cellH = screenHeight / GRID_ROWS
        val rows = GRID_ROWS + 1
        val cols = GRID_COLS + 1

        // Generate jittered grid points
        val points = Array(rows) { row ->
            Array(cols) { col ->
                val baseX = col * cellW
                val baseY = row * cellH
                val jitterX = (Random.nextFloat() - 0.5f) * GRID_JITTER * cellW
                val jitterY = (Random.nextFloat() - 0.5f) * GRID_JITTER * cellH
                Pair(baseX + jitterX, baseY + jitterY)
            }
        }

        val result = mutableListOf<CrackSegment>()

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val (x, y) = points[row][col]

                // Connect to right neighbor
                if (col < cols - 1) {
                    val (rx, ry) = points[row][col + 1]
                    result.add(CrackSegment(x, y, rx, ry, Random.nextFloat()))
                }

                // Connect to bottom neighbor
                if (row < rows - 1) {
                    val (bx, by) = points[row + 1][col]
                    result.add(CrackSegment(x, y, bx, by, Random.nextFloat()))
                }

                // Connect to bottom-right diagonal neighbor
                if (row < rows - 1 && col < cols - 1) {
                    val (dx, dy) = points[row + 1][col + 1]
                    result.add(CrackSegment(x, y, dx, dy, Random.nextFloat()))
                }
            }
        }

        return result
    }

    fun activateZap(centerX: Float, centerY: Float) {
        zapSegments = generateZapPattern(centerX, centerY, 120f)
        zapCoverage = 0f
        zapAlpha = 1f
        zapFlashAlpha = 1f
        zapActive = true
    }

    fun updateZap(deltaTime: Float) {
        if (!zapActive) return
        zapCoverage = (zapCoverage + 3f * deltaTime).coerceAtMost(1f)  // Fast: ~0.33s to full
        if (zapCoverage >= 1f) {
            zapAlpha -= 2f * deltaTime  // Fade over 0.5s
            if (zapAlpha <= 0f) {
                zapActive = false
            }
        }
        zapFlashAlpha = (zapFlashAlpha - 3f * deltaTime).coerceAtLeast(0f)
    }

    fun renderZap(canvas: Canvas, cameraX: Float, cameraY: Float, screenWidth: Float, screenHeight: Float) {
        if (!zapActive) return

        val alpha = zapAlpha.coerceIn(0f, 1f)

        for (seg in zapSegments) {
            val segProgress = ((zapCoverage - seg.delay) / 0.3f).coerceIn(0f, 1f)
            if (segProgress <= 0f) continue

            val midX = (seg.x1 + seg.x2) / 2f
            val midY = (seg.y1 + seg.y2) / 2f
            val drawX1 = midX + (seg.x1 - midX) * segProgress
            val drawY1 = midY + (seg.y1 - midY) * segProgress
            val drawX2 = midX + (seg.x2 - midX) * segProgress
            val drawY2 = midY + (seg.y2 - midY) * segProgress

            // World to screen
            val sx1 = drawX1 - cameraX
            val sy1 = drawY1 - cameraY
            val sx2 = drawX2 - cameraX
            val sy2 = drawY2 - cameraY

            // Glow pass (thick, transparent cyan)
            glowPaint.alpha = (alpha * 0.5f * 255).toInt().coerceIn(0, 255)
            canvas.drawLine(sx1, sy1, sx2, sy2, glowPaint)

            // Core pass (thin, bright)
            corePaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
            canvas.drawLine(sx1, sy1, sx2, sy2, corePaint)
        }

        // Central white flash
        if (zapFlashAlpha > 0f) {
            val cx = (zapSegments.firstOrNull()?.x1 ?: 0f)
            val cy = (zapSegments.firstOrNull()?.y1 ?: 0f)
            val sx = cx - cameraX
            val sy = cy - cameraY
            flashPaint.alpha = (zapFlashAlpha * 200).toInt().coerceIn(0, 255)
            canvas.drawCircle(sx, sy, 80f * zapFlashAlpha, flashPaint)
        }
    }

    /**
     * Render the Time Crystal orb in world space during the post-victory sequence.
     * Visual style matches the hangar crystal reveal orb.
     */
    fun renderTimeCrystalOrb(
        canvas: Canvas,
        state: GameState,
        cameraX: Float,
        cameraY: Float
    ) {
        if (state.timeCrystalPhase == GameState.TimeCrystalPhase.NONE) return

        val time = (System.currentTimeMillis() % 10000L) / 1000f
        val orbPulse = 0.7f + 0.3f * sin(time * 4f)

        val sx = state.timeCrystalX - cameraX
        val sy = state.timeCrystalY - cameraY

        orbPaint.color = CrystalPalette.MID

        when (state.timeCrystalPhase) {
            GameState.TimeCrystalPhase.RISING -> {
                // Crystal materializes over 2s — orb grows from nothing
                val t = (state.timeCrystalTimer / 2f).coerceIn(0f, 1f)
                orbPaint.alpha = (orbPulse * 100 * t).toInt()
                canvas.drawCircle(sx, sy, 10f * t, orbPaint)
                orbPaint.alpha = (orbPulse * 220 * t).toInt()
                canvas.drawCircle(sx, sy, 4f * t, orbPaint)
            }
            GameState.TimeCrystalPhase.HOVERING -> {
                // Floating orb with glow — same as RISING end state
                orbPaint.alpha = (orbPulse * 100).toInt()
                canvas.drawCircle(sx, sy, 10f, orbPaint)
                orbPaint.alpha = (orbPulse * 220).toInt()
                canvas.drawCircle(sx, sy, 4f, orbPaint)
            }
            GameState.TimeCrystalPhase.FLYING -> {
                val t = (state.timeCrystalTimer / 1.5f).coerceIn(0f, 1f)
                // Trailing circles behind (along arc path)
                val srcX = state.timeCrystalOriginX - cameraX
                val srcY = state.timeCrystalOriginY - cameraY
                val arcHeight = -80f
                for (i in 4 downTo 1) {
                    val trailT = (t - i * 0.04f).coerceAtLeast(0f)
                    val trailEased = if (trailT < 0.5f) 2f * trailT * trailT
                        else 1f - (-2f * trailT + 2f).let { it * it } / 2f
                    val trailX = srcX + (sx - srcX) * (trailEased / t.coerceAtLeast(0.01f))
                    val trailY = srcY + (sy - srcY) * (trailEased / t.coerceAtLeast(0.01f)) +
                        arcHeight * 4f * trailEased * (1f - trailEased)
                    orbPaint.alpha = ((1f - i / 5f) * 60).toInt()
                    canvas.drawCircle(trailX, trailY, 5f - i * 0.8f, orbPaint)
                }
                // Outer glow
                orbPaint.alpha = (orbPulse * 120).toInt()
                canvas.drawCircle(sx, sy, 10f, orbPaint)
                // Core
                orbPaint.alpha = (orbPulse * 240).toInt()
                canvas.drawCircle(sx, sy, 4f, orbPaint)
            }
            GameState.TimeCrystalPhase.COLLECTED -> {
                // Flash burst at collection point — hot white core flash
                val ft = (state.timeCrystalTimer / 0.3f).coerceIn(0f, 1f)
                val flashRadius = 40f * ft
                val flashAlpha = ((1f - ft) * 255).toInt()
                orbPaint.color = CrystalPalette.CORE
                orbPaint.alpha = flashAlpha
                canvas.drawCircle(sx, sy, flashRadius, orbPaint)
            }
            GameState.TimeCrystalPhase.NONE -> { /* no-op */ }
        }
    }

    private fun generateZapPattern(centerX: Float, centerY: Float, radius: Float): List<CrackSegment> {
        val result = mutableListOf<CrackSegment>()
        val rings = 4
        val pointsPerRing = 8

        val allPoints = mutableListOf<Pair<Float, Float>>()
        allPoints.add(Pair(centerX, centerY))

        for (ring in 1..rings) {
            val ringRadius = radius * ring / rings
            for (i in 0 until pointsPerRing) {
                val angle = (i.toFloat() / pointsPerRing) * 2f * kotlin.math.PI.toFloat() +
                    Random.nextFloat() * 0.4f
                val jitter = ringRadius * (0.8f + Random.nextFloat() * 0.4f)
                allPoints.add(Pair(
                    centerX + kotlin.math.cos(angle) * jitter,
                    centerY + kotlin.math.sin(angle) * jitter
                ))
            }
        }

        // Center to first ring
        for (i in 1..pointsPerRing) {
            result.add(CrackSegment(centerX, centerY, allPoints[i].first, allPoints[i].second,
                Random.nextFloat() * 0.3f))
        }

        // Ring-to-ring + within-ring connections
        for (ring in 1 until rings) {
            val ringStart = 1 + (ring - 1) * pointsPerRing
            val nextRingStart = 1 + ring * pointsPerRing
            for (i in 0 until pointsPerRing) {
                val from = allPoints[ringStart + i]
                val to = allPoints[nextRingStart + i]
                val delay = ring.toFloat() / rings * 0.5f + Random.nextFloat() * 0.2f
                result.add(CrackSegment(from.first, from.second, to.first, to.second, delay))

                val next = allPoints[ringStart + (i + 1) % pointsPerRing]
                result.add(CrackSegment(from.first, from.second, next.first, next.second, delay + 0.1f))
            }
        }

        return result
    }
}

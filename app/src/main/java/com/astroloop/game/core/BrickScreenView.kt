package com.astroloop.game.core

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.astroloop.game.data.HighScoreManager
import com.astroloop.game.data.PersistenceManager
import com.astroloop.game.render.FontManager

/**
 * The final screen. The loop is broken. The game is over.
 */
class BrickScreenView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private var thread: Thread? = null
    @Volatile
    private var running = false
    private var timer = 0f
    private var lastFrameTime = System.nanoTime()

    @Volatile
    private var statsRevealed = false
    private var statsTimer = 0f
    private var brickSoundPlayed = false
    // Cached stats
    private var cachedPlaytime = 0f
    private var cachedKills = 0
    private var cachedDeaths = 0
    private var cachedYenEarned = 0
    private var cachedEvolutions = 0
    private var cachedCasinoSpins = 0
    private var cachedStatLines: List<Pair<String, String>> = emptyList()

    private val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = FontManager.getRegular()
    }

    init {
        holder.addCallback(this)
        FontManager.initialize(context)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        lastFrameTime = System.nanoTime()
        thread = Thread {
            while (running) {
                val now = System.nanoTime()
                val delta = (now - lastFrameTime) / 1_000_000_000f
                lastFrameTime = now
                timer += delta
                if (statsRevealed) statsTimer += delta
                if (!brickSoundPlayed && timer >= 1f) {
                    brickSoundPlayed = true
                    SoundManager.playSFX("sfx_crystal_activate")
                }

                val canvas = holder.lockCanvas() ?: continue
                try {
                    render(canvas)
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }

                // Stop updating once fully faded in and stats done
                if (timer > 7.5f && (!statsRevealed || statsTimer > 1.5f)) {
                    val finalCanvas = holder.lockCanvas()
                    if (finalCanvas != null) {
                        try { render(finalCanvas) } finally { holder.unlockCanvasAndPost(finalCanvas) }
                    }
                    while (running) { Thread.sleep(1000) }
                }

                Thread.sleep(16)
            }
        }
        thread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        thread?.interrupt()
        try { thread?.join(1000) } catch (_: InterruptedException) {}
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && timer >= 7.5f && !statsRevealed) {
            statsTimer = 0f
            // Cache stats once — write all cached values BEFORE setting statsRevealed
            val persistence = PersistenceManager(context)
            val highScores = HighScoreManager(context)
            cachedPlaytime = highScores.getTotalPlayTime()
            cachedKills = persistence.getTotalKills()
            cachedDeaths = persistence.getTotalDeaths()
            cachedYenEarned = persistence.getTotalYenEarned()
            cachedEvolutions = persistence.getDiscoveredEvolutions().size
            cachedCasinoSpins = persistence.getTotalCasinoSpins()
            val hours = (cachedPlaytime / 3600).toInt()
            val mins = ((cachedPlaytime % 3600) / 60).toInt()
            cachedStatLines = listOf(
                "Time Played" to "${hours}h ${mins}m",
                "Kills" to "$cachedKills",
                "Deaths" to "$cachedDeaths",
                "Yen Earned" to "$cachedYenEarned",
                "Evolutions" to "$cachedEvolutions/12",
                "Casino Spins" to "$cachedCasinoSpins"
            )
            statsRevealed = true  // Set flag last so render thread sees cached values
        }
        return true
    }

    private fun render(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)

        textPaint.typeface = FontManager.getRegular()
        val cx = width / 2f
        val cy = height / 2f

        // "The loop is broken." — fades in at 1s, holds, fades out at 4s
        val line1Alpha = when {
            timer < 1f -> 0f
            timer < 2f -> timer - 1f
            timer < 4f -> 1f
            timer < 5f -> 1f - (timer - 4f)
            else -> 0f
        }

        if (line1Alpha > 0f) {
            textPaint.color = Color.WHITE
            textPaint.alpha = (line1Alpha * 255).toInt()
            canvas.drawText("The loop is broken.", cx, cy - 20f, textPaint)
        }

        // "Goodbye, commander." — fades in at 6s, stays
        val line2Alpha = when {
            timer < 6f -> 0f
            timer < 7.5f -> (timer - 6f) / 1.5f
            else -> 1f
        }

        // Compute goodbye Y position — animates up when stats revealed
        val goodbyeY = if (statsRevealed) {
            val t = (statsTimer / 0.5f).coerceIn(0f, 1f)
            val eased = 1f - (1f - t) * (1f - t)
            cy + 20f + (height * 0.25f - (cy + 20f)) * eased
        } else {
            cy + 20f
        }

        if (line2Alpha > 0f) {
            textPaint.color = 0xFF88AACC.toInt()  // TB-26's color
            textPaint.alpha = (line2Alpha * 255).toInt()
            canvas.drawText("Goodbye, commander.", cx, goodbyeY, textPaint)
        }

        // Career stats — fade in after goodbye finishes moving
        if (statsRevealed) {
            val statsAlpha = ((statsTimer - 0.5f) / 0.5f).coerceIn(0f, 1f)
            if (statsAlpha > 0f) {
                val alphaInt = (statsAlpha * 255).toInt()
                textPaint.textSize = 22f
                val startY = height * 0.42f
                val lineSpacing = 36f

                for ((i, stat) in cachedStatLines.withIndex()) {
                    val y = startY + i * lineSpacing

                    // Label in TB-26 blue, right-aligned
                    textPaint.textAlign = Paint.Align.RIGHT
                    textPaint.color = 0xFF88AACC.toInt()
                    textPaint.alpha = alphaInt
                    canvas.drawText(stat.first, cx - 20f, y, textPaint)

                    // Value in white, left-aligned
                    textPaint.textAlign = Paint.Align.LEFT
                    textPaint.color = 0xFFFFFFFF.toInt()
                    textPaint.alpha = alphaInt
                    canvas.drawText(stat.second, cx + 20f, y, textPaint)
                }

                // Reset paint state
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.textSize = 28f
            }
        }

    }
}

package com.astroloop.game.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.astroloop.game.core.Camera
import com.astroloop.game.core.GameConfig
import com.astroloop.game.util.Vector2
import kotlin.random.Random

data class Star(
    var x: Float,
    var y: Float,
    val size: Float,
    val color: Int,
    val speedFactor: Float
)

class StarfieldRenderer {

    private val stars = mutableListOf<Star>()
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private var screenWidth: Float = 0f
    private var screenHeight: Float = 0f
    private var initialized = false

    // Track cumulative camera offset for parallax
    private var lastCameraX: Float = 0f
    private var lastCameraY: Float = 0f

    // Boss mode - tints everything dark red
    var bossMode: Boolean = false

    // Offscreen bitmap cache — redrawn only when stars move visibly
    private var starBitmap: Bitmap? = null
    private var bitmapDirty = true
    private var lastBossMode = false
    private var accMoveX = 0f
    private var accMoveY = 0f

    fun initialize(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
        stars.clear()

        // Far stars (smallest, slowest)
        repeat(GameConfig.STARS_FAR_COUNT) {
            stars.add(Star(
                x = Random.nextFloat() * width,
                y = Random.nextFloat() * height,
                size = 1f + Random.nextFloat() * 0.5f,
                color = GameConfig.COLOR_STAR_FAR,
                speedFactor = GameConfig.STARS_FAR_SPEED_FACTOR
            ))
        }

        // Mid stars
        repeat(GameConfig.STARS_MID_COUNT) {
            stars.add(Star(
                x = Random.nextFloat() * width,
                y = Random.nextFloat() * height,
                size = 1.5f + Random.nextFloat() * 0.5f,
                color = GameConfig.COLOR_STAR_MID,
                speedFactor = GameConfig.STARS_MID_SPEED_FACTOR
            ))
        }

        // Near stars (largest, fastest)
        repeat(GameConfig.STARS_NEAR_COUNT) {
            stars.add(Star(
                x = Random.nextFloat() * width,
                y = Random.nextFloat() * height,
                size = 2f + Random.nextFloat(),
                color = GameConfig.COLOR_STAR_NEAR,
                speedFactor = GameConfig.STARS_NEAR_SPEED_FACTOR
            ))
        }

        initialized = true
        lastCameraX = 0f
        lastCameraY = 0f
        bitmapDirty = true
        accMoveX = 0f
        accMoveY = 0f
        starBitmap?.recycle()
        starBitmap = null
    }

    fun update(shipVelocity: Vector2, deltaTime: Float) {
        if (!initialized) return

        for (star in stars) {
            // Move stars opposite to ship direction (creates parallax)
            star.x -= shipVelocity.x * star.speedFactor * deltaTime
            star.y -= shipVelocity.y * star.speedFactor * deltaTime

            // Wrap around screen
            if (star.x < 0) star.x += screenWidth
            if (star.x > screenWidth) star.x -= screenWidth
            if (star.y < 0) star.y += screenHeight
            if (star.y > screenHeight) star.y -= screenHeight
        }
        // Track movement for bitmap dirty check (using near-star speed = fastest)
        accMoveX += kotlin.math.abs(shipVelocity.x * GameConfig.STARS_NEAR_SPEED_FACTOR * deltaTime)
        accMoveY += kotlin.math.abs(shipVelocity.y * GameConfig.STARS_NEAR_SPEED_FACTOR * deltaTime)
    }

    fun updateWithCamera(camera: Camera) {
        if (!initialized) return

        // Calculate camera delta
        val deltaX = camera.x - lastCameraX
        val deltaY = camera.y - lastCameraY
        lastCameraX = camera.x
        lastCameraY = camera.y

        for (star in stars) {
            // Move stars based on camera movement with parallax
            star.x -= deltaX * star.speedFactor
            star.y -= deltaY * star.speedFactor

            // Wrap around screen
            if (star.x < 0) star.x += screenWidth
            if (star.x > screenWidth) star.x -= screenWidth
            if (star.y < 0) star.y += screenHeight
            if (star.y > screenHeight) star.y -= screenHeight
        }
        accMoveX += kotlin.math.abs(deltaX * GameConfig.STARS_NEAR_SPEED_FACTOR)
        accMoveY += kotlin.math.abs(deltaY * GameConfig.STARS_NEAR_SPEED_FACTOR)
    }

    fun render(canvas: Canvas) {
        if (!initialized) return

        val needsRedraw = bitmapDirty || bossMode != lastBossMode || accMoveX > 2f || accMoveY > 2f
        if (needsRedraw) {
            val existing = starBitmap
            val bmp = if (existing != null && existing.width == screenWidth.toInt() && existing.height == screenHeight.toInt()) {
                existing
            } else {
                existing?.recycle()
                Bitmap.createBitmap(screenWidth.toInt(), screenHeight.toInt(), Bitmap.Config.ARGB_8888)
                    .also { starBitmap = it }
            }

            bmp.eraseColor(Color.TRANSPARENT)
            val bmpCanvas = android.graphics.Canvas(bmp)

            for (star in stars) {
                if (bossMode) {
                    val originalColor = star.color
                    val r = ((originalColor shr 16) and 0xFF)
                    val g = ((originalColor shr 8) and 0xFF)
                    val b = (originalColor and 0xFF)
                    val newR = ((r * 0.9f + 20).toInt()).coerceAtMost(255)
                    val newG = (g * 0.5f).toInt()
                    val newB = (b * 0.5f).toInt()
                    paint.color = (0xFF shl 24) or (newR shl 16) or (newG shl 8) or newB
                } else {
                    paint.color = star.color
                }
                bmpCanvas.drawCircle(star.x, star.y, star.size, paint)
            }

            bitmapDirty = false
            lastBossMode = bossMode
            accMoveX = 0f
            accMoveY = 0f
        }

        starBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
    }

    fun reset() {
        if (initialized) {
            starBitmap?.recycle()
            starBitmap = null
            initialize(screenWidth, screenHeight)
        }
    }
}

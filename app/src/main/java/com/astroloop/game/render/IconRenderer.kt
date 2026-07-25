package com.astroloop.game.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

object IconRenderer {

    private val bitmapPaint = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = true
    }

    fun drawIcon(
        canvas: Canvas,
        id: String,
        isWeapon: Boolean,
        x: Float,
        y: Float,
        size: Float,
        paint: Paint
    ) {
        val bitmap: Bitmap? = if (isWeapon) IconCache.getWeaponIcon(id)
                              else          IconCache.getPassiveIcon(id)
        bitmap ?: return

        val half = size / 2f
        val destRect = RectF(x - half, y - half, x + half, y + half)
        bitmapPaint.alpha = paint.alpha
        canvas.drawBitmap(bitmap, null, destRect, bitmapPaint)
        bitmapPaint.alpha = 255   // reset
    }
}

package com.astroloop.game.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.astroloop.game.util.Vector2
import kotlin.math.cos
import kotlin.math.sin

class ShapeRenderer {

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val fillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val path = Path()

    fun setStrokeWidth(width: Float) {
        paint.strokeWidth = width
    }

    fun setColor(color: Int) {
        paint.color = color
        fillPaint.color = color
    }

    fun setAlpha(alpha: Float) {
        paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        fillPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
    }

    fun drawLine(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) {
        canvas.drawLine(x1, y1, x2, y2, paint)
    }

    fun drawCircle(canvas: Canvas, x: Float, y: Float, radius: Float, filled: Boolean = false) {
        if (filled) {
            canvas.drawCircle(x, y, radius, fillPaint)
        } else {
            canvas.drawCircle(x, y, radius, paint)
        }
    }

    fun drawPolygon(canvas: Canvas, x: Float, y: Float, points: FloatArray, rotation: Float = 0f, filled: Boolean = false) {
        if (points.size < 4) return

        path.reset()

        val cos = cos(rotation)
        val sin = sin(rotation)

        for (i in points.indices step 2) {
            val px = points[i]
            val py = points[i + 1]

            // Rotate point
            val rx = px * cos - py * sin
            val ry = px * sin + py * cos

            if (i == 0) {
                path.moveTo(x + rx, y + ry)
            } else {
                path.lineTo(x + rx, y + ry)
            }
        }

        path.close()
        canvas.drawPath(path, if (filled) fillPaint else paint)
    }

    fun drawTriangle(canvas: Canvas, x: Float, y: Float, size: Float, rotation: Float) {
        val points = floatArrayOf(
            size, 0f,           // Nose
            -size * 0.7f, -size * 0.5f,  // Left wing
            -size * 0.4f, 0f,            // Back center
            -size * 0.7f, size * 0.5f    // Right wing
        )
        drawPolygon(canvas, x, y, points, rotation)
    }

    fun drawRect(canvas: Canvas, x: Float, y: Float, width: Float, height: Float, filled: Boolean = false) {
        if (filled) {
            canvas.drawRect(x, y, x + width, y + height, fillPaint)
        } else {
            canvas.drawRect(x, y, x + width, y + height, paint)
        }
    }

    fun drawArc(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngle: Float,
        sweepAngle: Float,
        segments: Int = 16
    ) {
        val angleStep = sweepAngle / segments

        var prevX = centerX + cos(startAngle) * radius
        var prevY = centerY + sin(startAngle) * radius

        for (i in 1..segments) {
            val angle = startAngle + angleStep * i
            val x = centerX + cos(angle) * radius
            val y = centerY + sin(angle) * radius
            canvas.drawLine(prevX, prevY, x, y, paint)
            prevX = x
            prevY = y
        }
    }

    fun drawHexagon(canvas: Canvas, x: Float, y: Float, radius: Float, rotation: Float = 0f) {
        val points = FloatArray(12)
        for (i in 0 until 6) {
            val angle = (i * Math.PI / 3).toFloat()
            points[i * 2] = cos(angle) * radius
            points[i * 2 + 1] = sin(angle) * radius
        }
        drawPolygon(canvas, x, y, points, rotation)
    }

    fun drawStar(canvas: Canvas, x: Float, y: Float, outerRadius: Float, innerRadius: Float, points: Int, rotation: Float = 0f) {
        val starPoints = FloatArray(points * 4)
        val angleStep = (Math.PI * 2 / points).toFloat()

        for (i in 0 until points) {
            val outerAngle = angleStep * i + rotation
            val innerAngle = outerAngle + angleStep / 2

            starPoints[i * 4] = cos(outerAngle) * outerRadius
            starPoints[i * 4 + 1] = sin(outerAngle) * outerRadius
            starPoints[i * 4 + 2] = cos(innerAngle) * innerRadius
            starPoints[i * 4 + 3] = sin(innerAngle) * innerRadius
        }

        path.reset()
        path.moveTo(x + starPoints[0], y + starPoints[1])
        for (i in 1 until points * 2) {
            val idx = (i * 2) % starPoints.size
            path.lineTo(x + starPoints[idx], y + starPoints[idx + 1])
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    fun drawDot(canvas: Canvas, x: Float, y: Float, radius: Float = 2f) {
        canvas.drawCircle(x, y, radius, fillPaint)
    }

    fun drawCross(canvas: Canvas, x: Float, y: Float, size: Float) {
        canvas.drawLine(x - size, y, x + size, y, paint)
        canvas.drawLine(x, y - size, x, y + size, paint)
    }

    fun drawDiamond(canvas: Canvas, x: Float, y: Float, size: Float, rotation: Float = 0f) {
        val points = floatArrayOf(
            0f, -size,
            size, 0f,
            0f, size,
            -size, 0f
        )
        drawPolygon(canvas, x, y, points, rotation)
    }

    fun drawPath(canvas: Canvas, path: Path, filled: Boolean = true) {
        if (filled) {
            canvas.drawPath(path, fillPaint)
        } else {
            canvas.drawPath(path, paint)
        }
    }
}

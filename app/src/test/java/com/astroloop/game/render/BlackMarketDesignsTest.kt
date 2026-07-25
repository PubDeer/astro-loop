package com.astroloop.game.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BlackMarketDesignsTest {

    @Test
    fun `names array length matches COUNT`() {
        assertEquals(BlackMarketDesigns.COUNT, BlackMarketDesigns.NAMES.size)
    }

    @Test
    fun `every design index renders without throwing`() {
        val bmp = Bitmap.createBitmap(400, 800, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bounds = RectF(0f, 0f, 400f, 800f)
        for (i in 0 until BlackMarketDesigns.COUNT) {
            BlackMarketDesigns.render(canvas, i, bounds)
            BlackMarketDesigns.drawMiniSlot(canvas, bounds)
        }
    }
}

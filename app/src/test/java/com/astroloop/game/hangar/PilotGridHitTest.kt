package com.astroloop.game.hangar

import com.astroloop.game.core.LayoutRect
import com.astroloop.game.core.ScreenLayout
import org.junit.Assert.*
import org.junit.Test

/**
 * Validates the mathematical pilot grid hit-detection logic mirrored from HangarRenderer.
 * Uses content-anchored geometry (via ScreenLayout.compute) so drawing and hit-detect
 * share a single source of truth, and fold-wide screens are tested explicitly.
 */
class PilotGridHitTest {

    private val gridPadding = 12f
    private val cardGap = 8f
    private val cols = 4
    private val rows = 3

    /** The grid's outer bounds, content-anchored — the single source of truth for the mirror. */
    private fun gridBounds(screenW: Float, screenH: Float): LayoutRect {
        val c = ScreenLayout.compute(screenW, screenH).content
        return LayoutRect(c.left + gridPadding, c.top + 70f, c.right - gridPadding, c.top + c.height * 0.52f)
    }

    // Mirror of HangarRenderer.getPilotGridIndex math — content-anchored.
    private fun hitIndex(x: Float, y: Float, screenW: Float, screenH: Float): Int? {
        val g = gridBounds(screenW, screenH)
        val cardWidth = (g.width - cardGap * (cols - 1)) / cols
        val cardHeight = (g.height - cardGap * (rows - 1)) / rows
        if (x < g.left || x > g.right || y < g.top || y > g.bottom) return null
        val col = ((x - g.left) / (cardWidth + cardGap)).toInt()
        val row = ((y - g.top) / (cardHeight + cardGap)).toInt()
        if (col >= cols || row >= rows) return null
        val withinCardX = (x - g.left) - col * (cardWidth + cardGap)
        val withinCardY = (y - g.top) - row * (cardHeight + cardGap)
        if (withinCardX > cardWidth || withinCardY > cardHeight) return null
        val index = row * cols + col
        if (index >= 12) return null
        return index
    }

    /** Returns the center (x, y) of the card at the given (col, row) using content geometry. */
    private fun cardCenter(col: Int, row: Int, screenW: Float, screenH: Float): Pair<Float, Float> {
        val g = gridBounds(screenW, screenH)
        val cardWidth = (g.width - cardGap * (cols - 1)) / cols
        val cardHeight = (g.height - cardGap * (rows - 1)) / rows
        val cx = g.left + col * (cardWidth + cardGap) + cardWidth / 2
        val cy = g.top + row * (cardHeight + cardGap) + cardHeight / 2
        return cx to cy
    }

    private val W = 1080f
    private val H = 1920f

    @Test
    fun `top-left card is index 0`() {
        val (cx, cy) = cardCenter(0, 0, W, H)
        assertEquals(0, hitIndex(cx, cy, W, H))
    }

    @Test
    fun `bottom-right card is index 11`() {
        val (cx, cy) = cardCenter(3, 2, W, H)
        assertEquals(11, hitIndex(cx, cy, W, H))
    }

    @Test
    fun `tap above grid returns null`() {
        val g = gridBounds(W, H)
        val (cx, _) = cardCenter(0, 0, W, H)
        assertNull(hitIndex(cx, g.top - 10f, W, H))
    }

    @Test
    fun `tap below grid returns null`() {
        val g = gridBounds(W, H)
        val (cx, _) = cardCenter(0, 0, W, H)
        assertNull(hitIndex(cx, g.bottom + 10f, W, H))
    }

    @Test
    fun `tap in horizontal gap returns null`() {
        val g = gridBounds(W, H)
        val cardWidth = (g.width - cardGap * (cols - 1)) / cols
        // x is in the gap between col 0 and col 1
        val gapX = g.left + cardWidth + cardGap / 2
        val centerY = g.top + g.height / 6f  // somewhere in row 0
        assertNull(hitIndex(gapX, centerY, W, H))
    }

    @Test
    fun `second row first column is index 4`() {
        val (cx, cy) = cardCenter(0, 1, W, H)
        assertEquals(4, hitIndex(cx, cy, W, H))
    }

    @Test
    fun `grid is centered horizontally on a fold-wide screen`() {
        val FW = 1500f; val FH = 2142f
        val content = ScreenLayout.compute(FW, FH).content
        // A point just inside the left background gutter (outside the centered block) misses.
        assertNull(hitIndex(content.left - 1f, 300f, FW, FH))
        // Column 0 center inside the content block hits index 0.
        val (cx, cy) = cardCenter(0, 0, FW, FH)
        assertEquals(0, hitIndex(cx, cy, FW, FH))
    }
}

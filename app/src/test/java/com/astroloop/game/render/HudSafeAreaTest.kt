package com.astroloop.game.render

import com.astroloop.game.core.LayoutRect
import com.astroloop.game.core.ScreenLayout
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the "combat HUD is never clipped by a cutout" guarantee. Mirrors the anchor
 * math in HUDRenderer.renderTopBar: every combat element is offset from `safe`, so each
 * must lie fully within `safe` on any device/inset combo. Keep constants in sync with HUDRenderer.
 */
class HudSafeAreaTest {
    private val padding = 20f
    private val iconSize = 48f
    private val iconPadding = 8f
    private val weaponSlots = 4
    private val yenAreaWidth = 140f
    private val barGap = 4f
    private val barHeight = (iconSize - barGap) / 2f

    private fun weaponGrid(safe: LayoutRect): LayoutRect {
        val left = safe.left + padding
        val top = safe.top + padding
        // 4 weapon slots across the top row
        return LayoutRect(left, top, left + weaponSlots * (iconSize + iconPadding), top + iconSize)
    }
    private fun healthBar(safe: LayoutRect): LayoutRect {
        val left = safe.left + padding
        val top = safe.top + padding
        val barStartX = left + weaponSlots * (iconSize + iconPadding) + 12f
        val rightX = safe.right - padding
        val barWidth = rightX - barStartX - yenAreaWidth
        return LayoutRect(barStartX, top, barStartX + barWidth, top + barHeight)
    }
    private fun timer(safe: LayoutRect): LayoutRect {
        // right-aligned at rightX, on the bottom grid row
        val rightX = safe.right - padding
        val top = safe.top + padding
        val bottomRowCenterY = top + iconSize + iconPadding + iconSize / 2f
        // Generous nominal width: HUDRenderer.formatTime() max is "M:SS" (~88px @ 36sp Silkscreen),
        // so 120px is a comfortable upper bound for the right-aligned timer label.
        return LayoutRect(rightX - 120f, bottomRowCenterY - 20f, rightX, bottomRowCenterY + 20f)
    }

    private fun assertInside(el: LayoutRect, safe: LayoutRect) {
        assertTrue("left ${el.left} < ${safe.left}", el.left >= safe.left)
        assertTrue("top ${el.top} < ${safe.top}", el.top >= safe.top)
        assertTrue("right ${el.right} > ${safe.right}", el.right <= safe.right)
        assertTrue("bottom ${el.bottom} > ${safe.bottom}", el.bottom <= safe.bottom)
    }

    @Test
    fun `combat HUD stays within safe area across devices and insets`() {
        val cases = listOf(
            ScreenLayout.compute(960f, 2142f),  // design-exact, zero insets (identity contract)
            ScreenLayout.compute(960f, 2142f, insetTop = 110f),
            ScreenLayout.compute(1500f, 2142f, insetTop = 90f, insetLeft = 40f),
            ScreenLayout.compute(960f, 2600f, insetTop = 60f, insetBottom = 48f),
            ScreenLayout.compute(2076f, 2152f, insetTop = 80f, insetRight = 60f)
        )
        for (l in cases) {
            assertInside(weaponGrid(l.safe), l.safe)
            assertInside(healthBar(l.safe), l.safe)
            assertInside(timer(l.safe), l.safe)
        }
    }
}

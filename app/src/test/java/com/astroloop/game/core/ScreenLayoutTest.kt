package com.astroloop.game.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenLayoutTest {

    private val designAspect = GameConfig.DESIGN_WIDTH / GameConfig.DESIGN_HEIGHT // 960/2142

    @Test
    fun `design-exact screen content fills the whole space`() {
        val l = ScreenLayout.compute(width = 960f, height = 2142f)
        assertEquals(0f, l.content.left, 0.01f)
        assertEquals(0f, l.content.top, 0.01f)
        assertEquals(960f, l.content.width, 0.01f)
        assertEquals(2142f, l.content.height, 0.01f)
    }

    @Test
    fun `wider-than-design screen centers a design-aspect content block horizontally`() {
        val width = 1500f
        val l = ScreenLayout.compute(width = width, height = 2142f)
        assertEquals(2142f, l.content.height, 0.01f)
        assertEquals(2142f * designAspect, l.content.width, 0.01f)
        assertEquals(width / 2f, l.content.centerX, 0.01f)
        assertTrue(l.content.left >= l.safe.left)
        assertTrue(l.content.right <= l.safe.right)
    }

    @Test
    fun `taller-than-design screen centers content block vertically`() {
        val height = 2600f
        val l = ScreenLayout.compute(width = 960f, height = height)
        assertEquals(960f, l.content.width, 0.01f)
        assertEquals(960f / designAspect, l.content.height, 0.01f)
        assertEquals(height / 2f, l.content.centerY, 0.01f)
    }

    @Test
    fun `content keeps the design aspect ratio on any size`() {
        for (w in listOf(720f, 960f, 1500f, 2076f, 3000f)) {
            for (h in listOf(1600f, 2142f, 2152f, 2600f)) {
                val l = ScreenLayout.compute(width = w, height = h)
                assertEquals(
                    "aspect wrong at ${w}x$h",
                    designAspect,
                    l.content.width / l.content.height,
                    0.001f
                )
            }
        }
    }

    @Test
    fun `safe rect excludes insets and content stays inside safe`() {
        val l = ScreenLayout.compute(
            width = 960f, height = 2142f,
            insetLeft = 0f, insetTop = 90f, insetRight = 0f, insetBottom = 40f
        )
        assertEquals(90f, l.safe.top, 0.01f)
        assertEquals(2142f - 40f, l.safe.bottom, 0.01f)
        assertTrue(l.content.top >= l.safe.top)
        assertTrue(l.content.bottom <= l.safe.bottom)
    }

    @Test
    fun `custom square designAspect on a square safe area yields square content`() {
        // Exercises the branch boundary (safeW/safeH == designAspect) and a non-default aspect.
        val l = ScreenLayout.compute(width = 1000f, height = 1000f, designAspect = 1f)
        assertEquals(1000f, l.content.width, 0.01f)
        assertEquals(1000f, l.content.height, 0.01f)
        assertEquals(500f, l.content.centerX, 0.01f)
        assertEquals(500f, l.content.centerY, 0.01f)
    }

    @Test
    fun `zero-size measurement does not crash and content falls back to safe`() {
        val l = ScreenLayout.compute(width = 0f, height = 0f)
        assertEquals(0f, l.content.width, 0.01f)
        assertEquals(0f, l.content.height, 0.01f)
    }

    @Test
    fun `full rect always spans the whole design space ignoring insets`() {
        val l = ScreenLayout.compute(width = 1200f, height = 2000f, insetTop = 50f)
        assertEquals(0f, l.full.left, 0.01f)
        assertEquals(0f, l.full.top, 0.01f)
        assertEquals(1200f, l.full.width, 0.01f)
        assertEquals(2000f, l.full.height, 0.01f)
    }
}

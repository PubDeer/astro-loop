package com.astroloop.game.hangar

import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.ScreenLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HangarMetricsTest {

    /** Mirrors HangarSurfaceView.applyScreenDimensions: physical px in, design units out. */
    private fun roomWidthFor(physW: Float, physH: Float, swDp: Int): Float {
        val renderScale = minOf(physW / GameConfig.DESIGN_WIDTH, physH / GameConfig.DESIGN_HEIGHT)
        val screenW = physW / renderScale
        val screenH = physH / renderScale
        val layout = ScreenLayout.compute(width = screenW, height = screenH)
        return HangarMetrics.roomWidth(screenW, layout.content.width, swDp)
    }

    @Test
    fun `below the gate the room is the whole screen`() {
        // Compact 16:9 phone. Content would be narrower, but the gate keeps today's layout.
        val renderScale = minOf(1080f / GameConfig.DESIGN_WIDTH, 1920f / GameConfig.DESIGN_HEIGHT)
        val screenW = 1080f / renderScale
        assertEquals(screenW, roomWidthFor(1080f, 1920f, swDp = 411), 0.01f)
    }

    @Test
    fun `at the gate the room narrows to the content column`() {
        // Landscape tablet. Content collapses to exactly DESIGN_WIDTH once wider than design aspect.
        assertEquals(GameConfig.DESIGN_WIDTH, roomWidthFor(2560f, 1600f, swDp = 800), 0.5f)
    }

    @Test
    fun `600dp is inclusive`() {
        val at599 = roomWidthFor(2560f, 1600f, swDp = 599)
        val at600 = roomWidthFor(2560f, 1600f, swDp = 600)
        assertEquals("599dp must keep the full-screen room", 3427f, at599, 2f)
        assertEquals("600dp must narrow to content", GameConfig.DESIGN_WIDTH, at600, 0.5f)
    }

    @Test
    fun `screens narrower than the design aspect are unaffected even above the gate`() {
        // 22:9 is narrower than 0.4482, so content already fills the width.
        val renderScale = minOf(1080f / GameConfig.DESIGN_WIDTH, 2640f / GameConfig.DESIGN_HEIGHT)
        val screenW = 1080f / renderScale
        assertEquals(screenW, roomWidthFor(1080f, 2640f, swDp = 700), 0.01f)
    }

    @Test
    fun `room width never exceeds the screen or collapses to zero`() {
        assertEquals(500f, HangarMetrics.roomWidth(500f, 9999f, 800), 0.01f)
        assertEquals(1f, HangarMetrics.roomWidth(500f, 0f, 800), 0.01f)
    }

    @Test
    fun `degenerate screen width passes through untouched`() {
        assertEquals(0f, HangarMetrics.roomWidth(0f, 0f, 800), 0.01f)
    }

    @Test
    fun `effectiveRoomWidth falls back to the screen until dimensions arrive`() {
        assertEquals(960f, HangarMetrics.effectiveRoomWidth(960f, 3427f), 0.01f)
        assertEquals(3427f, HangarMetrics.effectiveRoomWidth(0f, 3427f), 0.01f)
    }

    // =====================================================================
    // Viewport transform (world X of the left screen edge)
    // =====================================================================

    @Test
    fun `below the gate the viewport transform is the original page-per-screen one`() {
        val screenW = 1204.8f            // 16:9 phone in design units
        val roomW = screenW              // below the gate the room is the screen
        for (page in 0..2) {
            for (offset in listOf(-300f, 0f, 47.5f)) {
                assertEquals(
                    "page $page offset $offset must keep the shipped transform",
                    page * screenW + offset,
                    HangarMetrics.viewportX(page, offset, roomW, screenW),
                    0f   // exact: (screenWidth - stride) / 2f is exactly 0 here
                )
            }
        }
    }

    @Test
    fun `above the gate the viewport transform centres the current room`() {
        val screenW = 3427.5f
        val roomW = 960f
        for (page in 0..2) {
            // The current room's left edge lands half the leftover width in.
            val roomLeftOnScreen = page * roomW - HangarMetrics.viewportX(page, 0f, roomW, screenW)
            assertEquals((screenW - roomW) / 2f, roomLeftOnScreen, 0.01f)
        }
        // Dragging the page left moves the world right past the viewport, one-for-one.
        assertEquals(
            HangarMetrics.viewportX(1, 0f, roomW, screenW) + 120f,
            HangarMetrics.viewportX(1, 120f, roomW, screenW),
            0.01f
        )
    }

    @Test
    fun `a room-local X placed on screen by the renderer's own transform round-trips through toRoomX`() {
        // The renderer places room-local X `r` for page `p` at screen X:
        //     r + (p * stride - viewportX(p, offset, roomWidth, screenWidth))
        // toRoomX is what every hit test uses to undo that placement. If the two ever disagree —
        // a sign flip, a dropped offset term, the wrong roomWidth in one but not the other — a tap
        // on something the renderer just drew resolves to the wrong room-local X. Tolerance is
        // 0.01f design units: comparable magnitudes elsewhere in this file use the same bound, and
        // it's ~20x the float32 rounding accumulated over the handful of add/subtracts here (order
        // 1e-4 at screenW ~3427.5f) while still an order of magnitude tighter than any real bug in
        // this transform would produce (which throws the result off by roomOriginX or more).
        data class Screen(val label: String, val roomWidth: Float, val screenWidth: Float)
        val cases = listOf(
            Screen("below the gate", roomWidth = 1204.8f, screenWidth = 1204.8f),
            Screen("above the gate", roomWidth = 960f, screenWidth = 3427.5f)
        )
        // 0f is rest, the common case. A small non-zero residual is exactly what rubber-band
        // overscroll leaves behind mid-settle — that exact case (a stale non-zero offset reaching
        // a hit test) already caused a Critical regression earlier in this work. -120f stands in
        // for a typical in-progress drag.
        val offsets = listOf(0f, 3.7f, -120f)

        for (screen in cases) {
            val stride = HangarMetrics.effectiveRoomWidth(screen.roomWidth, screen.screenWidth)
            for (page in 0..2) {
                for (offset in offsets) {
                    val viewportX = HangarMetrics.viewportX(page, offset, screen.roomWidth, screen.screenWidth)
                    for (r in listOf(0f, 1f, stride * 0.5f, stride - 1f)) {
                        val screenX = r + (page * stride - viewportX)
                        val roundTripped = HangarMetrics.toRoomX(
                            screenX, screen.roomWidth, screen.screenWidth, offset
                        )
                        assertEquals(
                            "${screen.label}, page $page, offset $offset, r $r",
                            r,
                            roundTripped,
                            0.01f
                        )
                    }
                }
            }
        }
    }

    // =====================================================================
    // Screen X → room-local X (hit testing)
    // =====================================================================

    @Test
    fun `below the gate the hit-test conversion is the identity`() {
        val screenW = 1204.8f
        // Taps are dispatched at rest: pageScrollOffset is zeroed on ACTION_DOWN and again
        // before handleTap, so this is the only case that reaches a hit test on a phone.
        for (x in listOf(0f, 1f, 137.25f, 600f, screenW)) {
            assertEquals(x, HangarMetrics.toRoomX(x, screenW, screenW, 0f), 0f)
        }
    }

    @Test
    fun `below the gate the fallback room width is also the identity`() {
        // Frames before dimensions propagate: roomWidth is still 0 and falls back to the screen.
        assertEquals(410f, HangarMetrics.toRoomX(410f, 0f, 1204.8f, 0f), 0f)
    }

    @Test
    fun `above the gate a tap on the current room's left edge maps to zero`() {
        val screenW = 3427.5f
        val roomW = 960f
        val roomLeftOnScreen = (screenW - roomW) / 2f
        assertEquals(0f, HangarMetrics.toRoomX(roomLeftOnScreen, roomW, screenW, 0f), 0.01f)
        assertEquals(roomW, HangarMetrics.toRoomX(roomLeftOnScreen + roomW, roomW, screenW, 0f), 0.01f)
        // Mid-drag the room has moved left by the scroll offset; the conversion follows it.
        assertEquals(0f, HangarMetrics.toRoomX(roomLeftOnScreen - 50f, roomW, screenW, 50f), 0.01f)
    }

    // =====================================================================
    // Content column → room-local (layout inside a page's translate)
    // =====================================================================

    @Test
    fun `below the gate the content column keeps its screen-space position`() {
        // 16:9 phone: content is narrower than the screen, so content.left is NOT zero. The
        // bar and shop grids must stay anchored to it exactly as they ship.
        val renderScale = minOf(1080f / GameConfig.DESIGN_WIDTH, 1920f / GameConfig.DESIGN_HEIGHT)
        val screenW = 1080f / renderScale
        val content = ScreenLayout.compute(screenW, 1920f / renderScale).content
        assertTrue("this device must have a non-zero content.left to be a real test", content.left > 1f)
        assertEquals(content.left, HangarMetrics.contentXInRoom(content.left, screenW, screenW), 0f)
        assertEquals(content.right, HangarMetrics.contentXInRoom(content.right, screenW, screenW), 0f)
    }

    @Test
    fun `above the gate the content column becomes the room`() {
        // Landscape tablet: the room IS the content column, so the column spans 0..roomWidth
        // and a 12px-padded grid sits 2px inside the counter's 10px inset on both edges.
        val renderScale = minOf(2560f / GameConfig.DESIGN_WIDTH, 1600f / GameConfig.DESIGN_HEIGHT)
        val screenW = 2560f / renderScale
        val content = ScreenLayout.compute(screenW, 1600f / renderScale).content
        val roomW = HangarMetrics.roomWidth(screenW, content.width, smallestScreenWidthDp = 800)
        assertEquals(0f, HangarMetrics.contentXInRoom(content.left, roomW, screenW), 0.01f)
        assertEquals(roomW, HangarMetrics.contentXInRoom(content.right, roomW, screenW), 0.01f)
    }

    @Test
    fun `stool centres match the renderer's spacing`() {
        // The renderer draws 8 stools between barLeft (10) and barRight (roomWidth - 10),
        // at barLeft + spacing * s for s in 1..8, spacing = (barRight - barLeft) / 9.
        val roomWidth = 960f
        val barLeft = 10f
        val barRight = roomWidth - 10f
        val spacing = (barRight - barLeft) / 9f
        for (s in 1..8) {
            assertEquals(
                "stool $s must sit where the renderer draws it",
                barLeft + spacing * s,
                HangarMetrics.stoolCenterX(roomWidth, s),
                0.01f
            )
        }
    }
}

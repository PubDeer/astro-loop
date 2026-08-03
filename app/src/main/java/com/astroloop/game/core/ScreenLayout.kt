package com.astroloop.game.core

/** Plain-Kotlin rectangle (no Android deps) so all layout math is unit-testable. */
data class LayoutRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    fun contains(x: Float, y: Float): Boolean =
        x >= left && x <= right && y >= top && y <= bottom
}

/**
 * Resolution-independent layout. All values are in DESIGN units (post renderScale).
 *
 *  - [full]    entire design space; backgrounds / starfield / room floors bleed here.
 *  - [safe]    full space minus system-cutout insets; edge-anchored UI (HUD) pins here.
 *  - [content] design-aspect rect (DESIGN_WIDTH:DESIGN_HEIGHT) contained and centered
 *              inside [safe]; centered content blocks (grids, cards, carousel) lay out here.
 */
class ScreenLayout private constructor(
    val full: LayoutRect,
    val safe: LayoutRect,
    val content: LayoutRect
) {
    val width: Float get() = full.width
    val height: Float get() = full.height

    companion object {
        /**
         * Short-edge width, in dp, at or above which a screen counts as large.
         *
         * The single home for that threshold. Both features that care — the hangar's adjacent-room
         * layout and the combat HUD's width — read it from here, because they gate on the same
         * physical judgement: this screen is wider than the design was drawn for. Two independent
         * 600s could drift apart and leave a device where the hangar narrows but the HUD does not.
         *
         * It is also the breakpoint Android itself uses to decide whether to honour a portrait lock.
         */
        const val LARGE_SCREEN_MIN_SW_DP = 600

        fun compute(
            width: Float,
            height: Float,
            insetLeft: Float = 0f,
            insetTop: Float = 0f,
            insetRight: Float = 0f,
            insetBottom: Float = 0f,
            designAspect: Float = GameConfig.DESIGN_WIDTH / GameConfig.DESIGN_HEIGHT
        ): ScreenLayout {
            val full = LayoutRect(0f, 0f, width, height)
            val safe = LayoutRect(insetLeft, insetTop, width - insetRight, height - insetBottom)

            val safeW = safe.width
            val safeH = safe.height
            // Guard transient zero/negative measurements (surfaceChanged can fire with 0 dims,
            // and renderScale=0 yields NaN). Fall back to a degenerate content == safe rather
            // than dividing by zero / propagating NaN into renderers.
            if (safeW <= 0f || safeH <= 0f || designAspect <= 0f) {
                return ScreenLayout(full, safe, safe)
            }

            val contentW: Float
            val contentH: Float
            if (safeW / safeH > designAspect) {
                contentH = safeH
                contentW = safeH * designAspect
            } else {
                contentW = safeW
                contentH = safeW / designAspect
            }
            val cl = safe.left + (safeW - contentW) / 2f
            val ct = safe.top + (safeH - contentH) / 2f
            val content = LayoutRect(cl, ct, cl + contentW, ct + contentH)

            return ScreenLayout(full, safe, content)
        }
    }
}

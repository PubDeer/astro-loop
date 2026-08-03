package com.astroloop.game.hangar

import com.astroloop.game.core.ScreenLayout

/**
 * Horizontal metrics for the three-page hangar.
 *
 * A room is normally the whole screen, and the three pages tile edge to edge one screen apart.
 * On large screens the room narrows to the content column instead, so the counter and the pilot
 * grid share a width and the pages tile at that narrower stride — which puts the neighbouring
 * rooms partly on screen and keeps the walkway continuous for the same reason it always was.
 *
 * Below the gate this returns the screen width and nothing about the layout changes.
 */
object HangarMetrics {

    /**
     * Short-edge width, in dp, at which rooms narrow to the content column.
     *
     * The same breakpoint Android uses to decide whether to honour a portrait lock, so large
     * screens are one concept rather than two. The deciding reason is not aesthetic: anchoring a
     * 16:9 phone to the content column cuts the chat column from ~1060px to ~841px, below the
     * ~930px floor the authored bar-chatter line lengths assume, so lines that fit everywhere
     * today would start ellipsizing there.
     */
    const val ADJACENT_ROOMS_MIN_SW_DP = ScreenLayout.LARGE_SCREEN_MIN_SW_DP

    /**
     * Width of a single hangar room, in design units, which is also the page stride.
     *
     * [screenWidth] and [contentWidth] are design units (physical px / renderScale), matching
     * `HangarSurfaceView.screenWidth` and `layout.content.width`.
     */
    fun roomWidth(screenWidth: Float, contentWidth: Float, smallestScreenWidthDp: Int): Float {
        if (screenWidth <= 0f) return screenWidth
        if (smallestScreenWidthDp < ADJACENT_ROOMS_MIN_SW_DP) return screenWidth
        return contentWidth.coerceIn(1f, screenWidth)
    }

    /**
     * The room width to actually draw with, falling back to [screenWidth] for the frames before
     * dimensions have propagated. Every consumer goes through this rather than repeating the
     * check, so there is one definition of "not set yet".
     */
    fun effectiveRoomWidth(roomWidth: Float, screenWidth: Float): Float =
        if (roomWidth > 0f) roomWidth else screenWidth

    /**
     * X of the current room's left edge, in screen space.
     *
     * The three rooms are centred on the screen as a block, so the current room starts half the
     * leftover width in, less however far the page has been dragged. Below the gate
     * [effectiveRoomWidth] returns the screen width, the leftover is zero, and the room's origin
     * is the screen's origin (offset by the drag, exactly as before).
     */
    fun roomOriginX(roomWidth: Float, screenWidth: Float, pageScrollOffset: Float): Float =
        (screenWidth - effectiveRoomWidth(roomWidth, screenWidth)) / 2f - pageScrollOffset

    /**
     * World X of the left screen edge — subtract it from a world X to get a screen X.
     *
     * Every drawing pass that places rooms, the walkway or the pilot walker needs this exact
     * quantity; it was spelled out by hand in three places before, and each copy was one edit
     * away from disagreeing with the others.
     *
     * Below the gate the stride is the screen width, so this is `currentPage * screenWidth +
     * pageScrollOffset` — the original single-page-per-screen transform, unchanged.
     */
    fun viewportX(currentPage: Int, pageScrollOffset: Float, roomWidth: Float, screenWidth: Float): Float =
        currentPage * effectiveRoomWidth(roomWidth, screenWidth) -
            roomOriginX(roomWidth, screenWidth, pageScrollOffset)

    /**
     * Screen X → the current room's local X.
     *
     * Page renderers draw inside `canvas.translate(-xOffset, 0f)` and publish their tap rects in
     * that room-local space, so every hit test against one of those rects has to cross into it
     * first. Below the gate the room origin is 0 and the page is at rest when taps are handled,
     * so this is the identity and hit testing is bit-for-bit what it always was.
     */
    fun toRoomX(screenX: Float, roomWidth: Float, screenWidth: Float, pageScrollOffset: Float): Float =
        screenX - roomOriginX(roomWidth, screenWidth, pageScrollOffset)

    /**
     * A screen-space content-column X (`layout.content.left` / `.right`) in room-local units.
     *
     * `ScreenLayout` centres the content column in the safe area, so its coordinates are screen
     * space; used raw inside a page's translate they land a room-offset too far right. Above the
     * gate the room *is* the content column, so this collapses to `0 .. roomWidth` and the grids
     * span the room. Below the gate the room is the whole screen, the offset is zero, and this is
     * the identity — the content-anchored layout phones ship today is untouched.
     *
     * No scroll term: the page's own translate already carries it.
     */
    fun contentXInRoom(contentX: Float, roomWidth: Float, screenWidth: Float): Float =
        toRoomX(contentX, roomWidth, screenWidth, pageScrollOffset = 0f)

    /** Number of stools drawn along the counter. */
    const val STOOL_COUNT = 8

    /**
     * Centre X of stool [stool] (1..STOOL_COUNT) in room-local design units.
     *
     * Single source of truth: BarPageRenderer draws stools here and HangarState walks seated
     * crew here. These were duplicated, so moving one without the other would seat crew at
     * coordinates with no stool drawn under them.
     */
    fun stoolCenterX(roomWidth: Float, stool: Int): Float {
        val barLeft = 10f
        val barRight = roomWidth - 10f
        val spacing = (barRight - barLeft) / (STOOL_COUNT + 1)
        return barLeft + spacing * stool
    }
}

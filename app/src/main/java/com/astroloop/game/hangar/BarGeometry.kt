// app/src/main/java/com/astroloop/game/hangar/BarGeometry.kt
package com.astroloop.game.hangar

/**
 * Pure counter-strip geometry derived from the walkway Y (design units).
 * Approved slim-counter proportions: the slab is 16px tall (was 27), with all
 * decoration riding the counter top so a single constant re-proportions the bar.
 */
class BarGeometry(walkwayY: Float) {
    val counterTop: Float = walkwayY - COUNTER_HEIGHT   // slab top
    val counterBottom: Float = walkwayY + 2f            // unchanged floor edge
    val shelfY: Float = counterTop - 18f                // back shelf
    val lampY: Float = counterTop - 25f                 // hanging lamp row
    val stoolSeatY: Float = counterTop + 3f             // stool seat, just under the top
    val beerSurfaceY: Float = counterTop                // beer flush on the surface

    companion object {
        const val COUNTER_HEIGHT = 14f  // was 25 (barTop = walkwayY - 25); slab 27 → 16
    }
}

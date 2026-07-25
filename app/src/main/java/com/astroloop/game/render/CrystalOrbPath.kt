package com.astroloop.game.render

import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure geometry for the crystal-reveal orb: a corkscrew that spirals from Astro to
 * the store crystal tile, with the spiral radius decaying to 0 at both ends so it
 * starts and lands cleanly on the endpoints.
 */
object CrystalOrbPath {
    const val TRAVEL_DURATION = 1.4f   // ORB_TRAVEL phase length (s)
    const val FLASH_DURATION = 0.3f    // FLASH phase length (s)
    const val MAX_RADIUS = 36f         // peak spiral radius at mid-flight (px)
    // Number of corkscrew loops over the flight (aesthetic). Endpoint landing is
    // guaranteed by the radius envelope sin(PI*t)=0 at t=1, independent of TURNS;
    // a whole/half-integer value just keeps the spiral visually symmetric.
    const val TURNS = 2.5f

    /** Returns the orb (x, y) at progress t in [0,1] from (srcX,srcY) to (dstX,dstY). */
    fun position(t: Float, srcX: Float, srcY: Float, dstX: Float, dstY: Float): Pair<Float, Float> {
        val tc = t.coerceIn(0f, 1f)
        val eased = if (tc < 0.5f) 2f * tc * tc else 1f - (-2f * tc + 2f).let { it * it } / 2f
        val baseX = srcX + (dstX - srcX) * eased
        val baseY = srcY + (dstY - srcY) * eased
        val dx = dstX - srcX
        val dy = dstY - srcY
        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.0001f)
        val perpX = -dy / len
        val perpY = dx / len
        val radius = MAX_RADIUS * sin(PI * tc).toFloat()
        val offset = radius * sin(2.0 * PI * TURNS * tc).toFloat()
        return Pair(baseX + perpX * offset, baseY + perpY * offset)
    }
}

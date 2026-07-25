package com.astroloop.game.hangar

import kotlin.math.sin

/**
 * Irregular neon flicker, reproducing StorePageRenderer's BLACK MARKET sign:
 * a hash of (time/80) drops the sign dark ~3/23 of the time, otherwise it
 * breathes at 0.7 ± 0.3. Returns a 0..1 alpha multiplier.
 */
object SignFlicker {
    fun dim(timeMs: Long): Float {
        val seed = (timeMs / 80L).toInt()
        val hash = (seed * 2654435761L).toInt()
        return if ((hash % 23) < 3) 0.3f
               else 0.7f + 0.3f * sin(timeMs / 400.0).toFloat()
    }
}

package com.astroloop.game.render

/**
 * Single source of truth for the Time Crystal's color identity (icy cyan).
 * Matches the values CrystalRenderer already uses for the death freeze, so the
 * freeze is the reference and every other crystal/crystal-powered visual aligns to it.
 */
object CrystalPalette {
    const val DEEP = 0xFF44AACC.toInt()   // dim accents / secondary text
    const val MID  = 0xFF88EEFF.toInt()   // borders, glows, rings, beams
    const val ICE  = 0xFFCCEEFF.toInt()   // bright rings, names, highlights
    const val CORE = 0xFFFFFFFF.toInt()   // hottest cores / flashes / sparkle

    /**
     * Reckoning spiral layer colors — index == phase index (P1..P5).
     * Cool cyan → hot white tracks the crystal losing its composure: each phase
     * adds one layer permanently, so P5 shows all five at once.
     * P1 IS [MID] — the base drip is the crystal's own ambient color.
     */
    val LAYER_COLORS = listOf(
        MID,                   // P1 cyan
        0xFF5FA8FF.toInt(),    // P2 blue
        0xFF8F7FFF.toInt(),    // P3 indigo
        0xFFD07FFF.toInt(),    // P4 violet
        0xFFE8F6FF.toInt()     // P5 white
    )
}

package com.astroloop.game.system

object CrystalReckoning {
    const val PILOT_ASTRO = "pilot_astro"
    const val BANDANA_TOTAL = 12

    /** True iff Astro's launch in Astro Loop with all bandanas should enter the one-time fight. */
    fun shouldEnter(isAstroLoop: Boolean, pilotId: String, bandanaCount: Int, crystalReleased: Boolean): Boolean =
        isAstroLoop && pilotId == PILOT_ASTRO && bandanaCount >= BANDANA_TOTAL && !crystalReleased
}

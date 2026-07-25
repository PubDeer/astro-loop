package com.astroloop.game.data

/**
 * Per-pilot bandana accent colors. Each is the high-contrast complement of the
 * pilot's signature color. Used for the code-drawn walker-dot band; the grid
 * portraits use the PNG bandana set.
 */
object BandanaDefinitions {

    private val accentColors: Map<String, Int> = mapOf(
        "pilot_medic"    to 0xFF2CC9B8.toInt(), // teal
        "pilot_rascal"   to 0xFF3D6EE8.toInt(), // cobalt
        "pilot_brutus"   to 0xFFE23D52.toInt(), // crimson
        "pilot_frost"    to 0xFFFF8A33.toInt(), // orange
        "pilot_dash"     to 0xFF8A4DE0.toInt(), // violet
        "pilot_ember"    to 0xFF2BC7E0.toInt(), // cyan
        "pilot_fang"     to 0xFFE0C233.toInt(), // gold
        "pilot_kraken"   to 0xFFFF6A4D.toInt(), // coral
        "pilot_whiskers" to 0xFF3D8FCC.toInt(), // steel blue
        "pilot_unit7"    to 0xFFFF4DB0.toInt(), // magenta
        "pilot_havoc"    to 0xFF9A4DE0.toInt(), // violet
        "pilot_astro"    to 0xFF33D6CC.toInt()  // cyan
    )

    fun accentColor(pilotId: String): Int = accentColors[pilotId] ?: 0xFFFFFFFF.toInt()
}

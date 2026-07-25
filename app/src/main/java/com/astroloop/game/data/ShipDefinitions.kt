package com.astroloop.game.data

data class ShipDef(
    val id: String,
    val name: String,
    val color: Int,
    val startingWeaponId: String,
    val cost: Int
)

object ShipDefinitions {
    // Note: Some ship IDs don't match their current colors due to a color redistribution
    // (2026-02-10). IDs kept unchanged to preserve save data compatibility.
    val ships = listOf(
        ShipDef("ship_blue", "Scout", 0xFF3388FF.toInt(), "pulse_cannon", 0),
        ShipDef("ship_green", "Tracer", 0xFF33FF77.toInt(), "homing_missiles", 3000),
        ShipDef("ship_orange", "Shrapnel", 0xFFFF8833.toInt(), "scatter_shot", 5000),
        ShipDef("ship_cyan", "Sentinel", 0xFF33FFEE.toInt(), "ion_orbiters", 8000),
        ShipDef("ship_lime", "Devastator", 0xFF6644FF.toInt(), "flak_cannon", 12000),
        ShipDef("ship_yellow", "Trap", 0xFFFFFF33.toInt(), "space_mines", 20000),
        ShipDef("ship_red", "Nova", 0xFFFF77AA.toInt(), "nova_blast", 30000),
        ShipDef("ship_magenta", "Ripper", 0xFF9933FF.toInt(), "energy_saw", 40000),
        ShipDef("ship_coral", "Tempest", 0xFFFFAA33.toInt(), "solar_storm", 55000),
        ShipDef("ship_indigo", "Dreadnought", 0xFFAAFF33.toInt(), "cluster_bomb", 70000),
        ShipDef("ship_purple", "Hedgehog", 0xFFFF33BB.toInt(), "needle_gun", 85000),
        ShipDef("ship_white", "Specter", 0xFFFFFFFF.toInt(), "railgun", 100000)
    )

    fun getShip(id: String): ShipDef? = ships.find { it.id == id }
    fun getShipByIndex(index: Int): ShipDef? = ships.getOrNull(index)
    fun getShipCount(): Int = ships.size

    fun getShipName(index: Int): String {
        return ships.getOrNull(index)?.name ?: "Unknown"
    }

    /** Get the color of the ship that starts with this weapon. Returns corruption red during corruption runs. */
    fun getWeaponColor(weaponId: String, isCorruption: Boolean = false): Int {
        if (isCorruption) return 0xFFAA2222.toInt() // Boss.CORRUPTION_COLOR
        return ships.find { it.startingWeaponId == weaponId }?.color ?: 0xFFFFFFFF.toInt()
    }

    /** Get a lighter tint of a color (for evolution weapons). Blends 40% toward white. */
    fun lightenColor(color: Int): Int {
        val a = (color ushr 24) and 0xFF
        val r = ((color ushr 16) and 0xFF).let { it + ((255 - it) * 0.4f).toInt() }
        val g = ((color ushr 8) and 0xFF).let { it + ((255 - it) * 0.4f).toInt() }
        val b = (color and 0xFF).let { it + ((255 - it) * 0.4f).toInt() }
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    /**
     * Evolution weapon color: a 50/50 RGB mix of the base weapon's ship color and the
     * evolution passive's pilot color — the weapon and passive that fuse into the evolution.
     * Corruption runs keep the lightened red.
     */
    fun getEvolutionColor(baseWeaponId: String, isCorruption: Boolean = false): Int {
        if (isCorruption) return lightenColor(getWeaponColor(baseWeaponId, true))
        return evolutionColors[baseWeaponId] ?: lightenColor(getWeaponColor(baseWeaponId, false))
    }

    // Each evolution's color = base weapon's ship color mixed 50/50 with its evolution
    // passive's pilot color (see PilotDefinitions). Keep these in sync with the icon PNGs.
    private val evolutionColors: Map<String, Int> = mapOf(
        "pulse_cannon"    to 0xFF3CBBAA.toInt(), // Storm Cannon    = Scout blue + Unit-7 green
        "energy_saw"      to 0xFFCC8891.toInt(), // Warp Saw        = Ripper purple + Dash yellow
        "scatter_shot"    to 0xFFC46680.toInt(), // Leech Burst     = Shrapnel orange + Fang purple
        "homing_missiles" to 0xFF44E66F.toInt(), // Autonomous Ace = Tracer green + Combat-Drone green (Astro/TB-26-X uses steel blue, see AutonomousAce)
        "ion_orbiters"    to 0xFF44DDF7.toInt(), // Frost Ring      = Sentinel cyan + Frost blue
        "railgun"         to 0xFFDDFF91.toInt(), // Oblivion Beam   = Specter white + Havoc lime
        "space_mines"     to 0xFFFFDD5E.toInt(), // Gambler's Mines = Trap yellow + Whiskers peach
        "solar_storm"     to 0xFFFF882B.toInt(), // Phoenix Flare   = Tempest orange + Ember red-orange
        "nova_blast"      to 0xFFBB916F.toInt(), // Lingering Nova  = Nova pink + Brutus olive
        "needle_gun"      to 0xFFFF5EB3.toInt(), // Siphon Needles  = Hedgehog magenta + Medic pink
        "cluster_bomb"    to 0xFFC4D533.toInt(), // Hunter-Killer   = Dreadnought lime + Rascal gold
        "flak_cannon"     to 0xFF4D77D5.toInt()  // Flak Barrage    = Devastator indigo + Kraken teal
    )
}

package com.astroloop.game.render

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.astroloop.game.data.PilotDefinitions

object IconCache {

    private val bitmaps = mutableMapOf<String, Bitmap>()
    @Volatile private var loaded = false

    private const val ICON_SIZE = 64
    private const val PORTRAIT_SIZE = 128

    // --- Public mapping helpers (tested directly) ---

    fun pilotIdToFilename(id: String): String = when (id) {
        "tb26" -> "portrait_tb26"
        else   -> "portrait_${id.removePrefix("pilot_")}"
    }

    fun bandanaPortraitFilename(id: String): String = "portrait_${id.removePrefix("pilot_")}_bandana"

    fun storeIdToFilename(id: String?): String? = when (id) {
        "health"    -> "salvage_plate"
        "shields"   -> "deflector_rig"
        "speed"     -> "nitro_boost"
        "damage"    -> "hot_rounds"
        "crit"      -> "lucky_rounds"
        "magnet"    -> "haul_line"
        "yen_bonus" -> "finders_fee"
        "salvage"   -> "scavenger_rig"
        "emergency_shield" -> "emergency_shield"
        null        -> "time_crystal"
        else        -> null   // unknown ID — caller's bug
    }

    fun slotSymbolToFilename(sym: Int): String = when (sym) {
        0 -> "yen"
        1 -> "star"
        2 -> "diamond"
        3 -> "rocket"
        4 -> "bolt"
        else -> "gear"
    }

    // --- Preload ---

    fun preload(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val am = context.assets

            for (id in WEAPON_IDS)
                load(am, "icons/weapons/icon_weapon_$id.png", ICON_SIZE, "weapon_$id")

            for (id in PASSIVE_IDS)
                load(am, "icons/passives/icon_passive_$id.png", ICON_SIZE, "passive_$id")

            for (pilotId in PILOT_IDS) {
                val file = pilotIdToFilename(pilotId)
                load(am, "icons/portraits/$file.png", PORTRAIT_SIZE, "portrait_$pilotId")
                // Bandana variant — silently skipped by load() until the PNGs ship.
                load(am, "icons/portraits/${bandanaPortraitFilename(pilotId)}.png", PORTRAIT_SIZE, "bandana_$pilotId")
            }
            for (pilotId in CORRUPTED_IDS) {
                val stripped = pilotId.removePrefix("pilot_")
                load(am, "icons/portraits/portrait_corrupted_$stripped.png", PORTRAIT_SIZE, "corrupted_$pilotId")
            }
            for (pilotId in GHOST_IDS) {
                val stripped = pilotId.removePrefix("pilot_")
                load(am, "icons/portraits/portrait_ghost_$stripped.png", PORTRAIT_SIZE, "ghost_$pilotId")
            }
            load(am, "icons/portraits/portrait_boss.png",   PORTRAIT_SIZE, "portrait_boss")
            load(am, "icons/portraits/portrait_locked.png", PORTRAIT_SIZE, "portrait_locked")
            load(am, "icons/portraits/portrait_tb26.png",          PORTRAIT_SIZE, "portrait_tb26")
            load(am, "icons/portraits/portrait_tb.png",            PORTRAIT_SIZE, "portrait_tb")
            load(am, "icons/portraits/portrait_command.png",       PORTRAIT_SIZE, "portrait_command")
            load(am, "icons/portraits/portrait_time_crystal.png",  PORTRAIT_SIZE, "portrait_time_crystal")

            for (sym in 0..5)
                load(am, "icons/slot/icon_slot_${slotSymbolToFilename(sym)}.png", ICON_SIZE, "slot_$sym")

            for (id in STORE_IDS) {
                val file = storeIdToFilename(id) ?: continue
                load(am, "icons/store/icon_store_$file.png", ICON_SIZE, "store_$id")
            }
            load(am, "icons/store/icon_store_time_crystal.png", ICON_SIZE, "store_time_crystal")
            load(am, "icons/store/icon_store_emergency_shield.png", ICON_SIZE, "store_emergency_shield")

            loaded = true
        }
    }

    private fun load(am: AssetManager, path: String, size: Int, key: String) {
        try {
            val raw = am.open(path).use { BitmapFactory.decodeStream(it) } ?: return
            // Two-step downscale: halve repeatedly until source is ≤ 2× target, then scale to final
            var src = raw
            while (src.width > size * 2) {
                val half = Bitmap.createScaledBitmap(src, src.width / 2, src.height / 2, true)
                if (src !== raw) src.recycle()
                src = half
            }
            val scaled = if (src.width != size || src.height != size) {
                Bitmap.createScaledBitmap(src, size, size, true)
            } else src
            if (src !== raw && src !== scaled) src.recycle()
            if (scaled !== raw) raw.recycle()
            bitmaps[key] = scaled
        } catch (e: Exception) {
            // Missing asset — silently skip
        }
    }

    // --- Accessors ---

    fun getWeaponIcon(id: String): Bitmap?    = bitmaps["weapon_$id"]
    fun getPassiveIcon(id: String): Bitmap?   = bitmaps["passive_$id"]
    fun getPortrait(pilotId: String): Bitmap? = bitmaps["portrait_$pilotId"]
    fun getBandanaPortrait(pilotId: String): Bitmap? = bitmaps["bandana_$pilotId"]
    fun getPortraitLocked(): Bitmap?           = bitmaps["portrait_locked"]
    fun getPortraitBoss(): Bitmap?             = bitmaps["portrait_boss"]
    fun getCorruptedPortrait(pilotId: String): Bitmap? = bitmaps["corrupted_$pilotId"]
    fun getGhostPortrait(pilotId: String): Bitmap? = bitmaps["ghost_$pilotId"]
    fun getSlotSymbol(sym: Int): Bitmap?       = bitmaps["slot_$sym"]
    // Keys stored as "store_$upgradeId" during preload — do NOT route through storeIdToFilename here.
    // storeIdToFilename is only for building the file path during preload.
    fun getStoreIcon(upgradeId: String?): Bitmap? {
        if (upgradeId == null) return bitmaps["store_time_crystal"]
        return bitmaps["store_$upgradeId"]
    }

    fun getPortraitByCallsign(callsign: String?, isCorrupted: Boolean = false,
                              bandanaPilotId: String? = null, isGhost: Boolean = false): Bitmap? {
        if (callsign == null) return null
        val pilot = PilotDefinitions.pilots.find { it.callsign == callsign }
        return when {
            isGhost && pilot != null -> getGhostPortrait(pilot.id) ?: getPortrait(pilot.id)
            isCorrupted && pilot != null -> getCorruptedPortrait(pilot.id)
            pilot != null && pilot.id == bandanaPilotId ->
                getBandanaPortrait(pilot.id) ?: getPortrait(pilot.id)
            pilot != null                -> getPortrait(pilot.id)
            callsign == "TB-26"          -> bitmaps["portrait_tb26"]
            callsign == "TOBAR"          -> bitmaps["portrait_tb"]
            callsign == "COMMAND"        -> bitmaps["portrait_command"]
            callsign == "CRYSTAL"        -> bitmaps["portrait_time_crystal"]
            else                         -> null
        }
    }

    fun recycle() {
        bitmaps.values.forEach { it.recycle() }
        bitmaps.clear()
        loaded = false
    }

    // --- ID lists ---

    private val WEAPON_IDS = listOf(
        "pulse_cannon", "energy_saw", "scatter_shot", "homing_missiles",
        "ion_orbiters", "railgun", "space_mines", "solar_storm",
        "nova_blast", "needle_gun", "cluster_bomb", "flak_cannon",
        "storm_cannon", "warp_saw", "leech_burst", "autonomous_ace",
        "frost_ring", "oblivion_beam", "jackpot_mines", "phoenix_flare",
        "lingering_nova", "siphon_needles", "hunter_killer", "flak_barrage",
        "tb26_x"   // Astro variant of Autonomous Ace (pilot-aware icon)
    )

    private val PASSIVE_IDS = listOf(
        "nano_repair", "duplicator_core", "magnet_field", "phoenix_core",
        "extra_weapon_slot", "tb26", "combat_drone", "momentum_drive", "cryo_field",
        "lucky_star", "revenge_protocol", "vampiric_core", "glass_cannon"
    )

    private val PILOT_IDS = listOf(
        "pilot_medic", "pilot_rascal", "pilot_brutus", "pilot_frost",
        "pilot_dash", "pilot_ember", "pilot_fang", "pilot_kraken",
        "pilot_whiskers", "pilot_unit7", "pilot_havoc", "pilot_astro"
    )

    private val CORRUPTED_IDS = listOf(
        "pilot_medic", "pilot_rascal", "pilot_brutus", "pilot_frost",
        "pilot_dash", "pilot_ember", "pilot_fang", "pilot_kraken",
        "pilot_whiskers", "pilot_unit7", "pilot_havoc"
    )

    // Ghost portraits exist only for the reckoning's crew voices (CrystalFightLines.ghostScript).
    // Base art is the CORRUPTION variants, recoloured — the crystal is holding them, so they wear
    // its mark, not their bandanas.
    private val GHOST_IDS = listOf("pilot_medic", "pilot_dash", "pilot_brutus", "pilot_whiskers")

    private val STORE_IDS = listOf("health", "shields", "speed", "damage", "crit", "magnet", "yen_bonus", "salvage")
}

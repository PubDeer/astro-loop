package com.astroloop.game.data

data class PilotDef(
    val id: String,
    val callsign: String,
    val type: String,
    val startingPassiveId: String,
    val color: Int,
    val unlockType: PilotUnlockType,
    val unlockThreshold: Int = 0
)

enum class PilotUnlockType {
    FREE,
    TOTAL_YEN_EARNED,
    TOTAL_DAMAGE_TAKEN,
    SURVIVE_SECONDS,
    KILL_STREAK,
    TOTAL_DEATHS,
    KILLS_IN_SINGLE_RUN,
    WEAPONS_DISCOVERED,
    EVOLUTIONS_DISCOVERED,
    TOTAL_KILLS,
    JACKPOT,
    ALL_OTHERS,
    CONTINUOUS_FLIGHT_SECONDS
}

object PilotDefinitions {
    val pilots = listOf(
        PilotDef("pilot_medic", "MEDIC", "Human", "nano_repair", 0xFFFF88AA.toInt(), PilotUnlockType.FREE),
        PilotDef("pilot_rascal", "RASCAL", "Raccoon", "magnet_field", 0xFFDDAA33.toInt(), PilotUnlockType.TOTAL_YEN_EARNED, 5000),
        PilotDef("pilot_brutus", "BRUTUS", "Bear", "revenge_protocol", 0xFF77AA33.toInt(), PilotUnlockType.TOTAL_DAMAGE_TAKEN, 500),
        PilotDef("pilot_frost", "FROST", "Penguin", "cryo_field", 0xFF55BBFF.toInt(), PilotUnlockType.SURVIVE_SECONDS, 300),
        PilotDef("pilot_dash", "DASH", "Cheetah", "momentum_drive", 0xFFFFDD22.toInt(), PilotUnlockType.CONTINUOUS_FLIGHT_SECONDS, 30),
        PilotDef("pilot_ember", "EMBER", "Phoenix", "phoenix_core", 0xFFFF6622.toInt(), PilotUnlockType.TOTAL_DEATHS, 18),
        PilotDef("pilot_fang", "FANG", "Bat", "vampiric_core", 0xFF8844CC.toInt(), PilotUnlockType.KILLS_IN_SINGLE_RUN, 5),
        PilotDef("pilot_kraken", "KRAKEN", "Octopus", "extra_weapon_slot", 0xFF33AAAA.toInt(), PilotUnlockType.WEAPONS_DISCOVERED, 6),
        PilotDef("pilot_whiskers", "WHISKERS", "Cat", "lucky_star", 0xFFFFBB88.toInt(), PilotUnlockType.JACKPOT, 0),
        PilotDef("pilot_unit7", "UNIT-7", "Robot", "duplicator_core", 0xFF44EE55.toInt(), PilotUnlockType.TOTAL_KILLS, 100),
        PilotDef("pilot_havoc", "HAVOC", "Human", "glass_cannon", 0xFFBBFF22.toInt(), PilotUnlockType.SURVIVE_SECONDS, 540),
        PilotDef("pilot_astro", "ASTRO", "Human", "tb26", 0xFFDD3333.toInt(), PilotUnlockType.ALL_OTHERS)
    )

    fun getPilot(id: String): PilotDef? = pilots.find { it.id == id }
    fun getPilotByIndex(index: Int): PilotDef? = pilots.getOrNull(index)
    fun getPilotCount(): Int = pilots.size
}

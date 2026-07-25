package com.astroloop.game.data

data class PassiveDef(
    val id: String,
    val name: String,
    val description: String,
    val effectPerStack: String
)

object PassiveDefinitions {

    val passives = listOf(
        PassiveDef(
            id = "nano_repair",
            name = "Nano Repair",
            description = "Regenerates health\nover time",
            effectPerStack = "+0.4 HP/sec"
        ),
        PassiveDef(
            id = "duplicator_core",
            name = "Duplicator Core",
            description = "Adds an extra\nprojectile",
            effectPerStack = "+1 projectile to all weapons"
        ),
        PassiveDef(
            id = "magnet_field",
            name = "Magnet Field",
            description = "Increases pickup range",
            effectPerStack = "+30% pickup range"
        ),
        PassiveDef(
            id = "phoenix_core",
            name = "Phoenix Core",
            description = "Resurrect once on death",
            effectPerStack = "One-time revival"
        ),
        PassiveDef(
            id = "extra_weapon_slot",
            name = "Weapon Expansion",
            description = "Gain a 5th slot\nlose one passive",
            effectPerStack = "+1 weapon\n-1 passive"
        ),
        PassiveDef(
            id = "tb26",
            name = "TB-26",
            description = "Fly with TB-26",
            effectPerStack = "+1 drone"
        ),
        PassiveDef(
            id = "combat_drone",
            name = "Combat Drone",
            description = "Deploy Combat Drone",
            effectPerStack = "+1 drone"
        ),
        PassiveDef(
            id = "momentum_drive",
            name = "Momentum Drive",
            description = "Damage increases\nwhile moving",
            effectPerStack = "+8% damage\nwhile moving"
        ),
        PassiveDef(
            id = "cryo_field",
            name = "Cryo Field",
            description = "Slow zone\naround ship",
            effectPerStack = "+25% radius"
        ),
        PassiveDef(
            id = "lucky_star",
            name = "Lucky Star",
            description = "More upgrades drop\nall picked randomly",
            effectPerStack = "Auto upgrades,\n+50% drops"
        ),
        PassiveDef(
            id = "revenge_protocol",
            name = "Revenge Protocol",
            description = "Retaliatory fire rate burst",
            effectPerStack = "2× fire rate\non hit"
        ),
        PassiveDef(
            id = "vampiric_core",
            name = "Vampiric Core",
            description = "Drain life from\nnearby asteroids",
            effectPerStack = "+0.1 HP/tick\nper asteroid"
        ),
        PassiveDef(
            id = "glass_cannon",
            name = "Glass Cannon",
            description = "No shields,\nmassive damage",
            effectPerStack = "+100% damage, shields disabled"
        )
    )

    fun getPassiveDef(id: String): PassiveDef? {
        return passives.find { it.id == id }
    }

    fun getAllPassives(): List<PassiveDef> = passives

    const val ASTRO_PILOT_ID = "pilot_astro"
    const val DRONE_COLOR_COMBAT = 0xFF55CC66.toInt()   // Tactical green
    const val DRONE_COLOR_TB26 = 0xFF88AACC.toInt()      // Steel blue

    fun getDisplayName(passiveId: String, activePilotId: String, isAstroLoopRun: Boolean = false): String {
        if (passiveId == "tb26") {
            return if (activePilotId == ASTRO_PILOT_ID && !isAstroLoopRun) "TB-26" else "Combat Drone"
        }
        return getPassiveDef(passiveId)?.name ?: passiveId
    }

    fun getDroneColor(passiveId: String, activePilotId: String, isAstroLoopRun: Boolean = false): Int {
        return if (passiveId == "tb26" && activePilotId == ASTRO_PILOT_ID && !isAstroLoopRun)
            DRONE_COLOR_TB26
        else
            DRONE_COLOR_COMBAT
    }

    fun getEffectivePassiveId(passiveId: String, activePilotId: String, isAstroLoopRun: Boolean = false): String {
        return if (passiveId == "tb26" && activePilotId == ASTRO_PILOT_ID && isAstroLoopRun)
            "combat_drone"
        else passiveId
    }
}

package com.astroloop.game.data

data class WeaponDef(
    val id: String,
    val name: String,
    val description: String,
    val baseDamage: Float,
    val baseCooldown: Float,
    val baseProjectileSpeed: Float,
    val baseProjectileCount: Int,
    val evolutionPassive: String? = null,
    val evolutionWeaponId: String? = null,
    val levelBonuses: List<String> = listOf(
        "Base weapon",
        "+30% damage",
        "+1 projectile",
        "+25% damage, +1 projectile",
        "+1 projectile, MAX LEVEL"
    )
) {
    fun getLevelDescription(level: Int): String {
        return levelBonuses.getOrElse(level - 1) { "MAX" }
    }
}

object WeaponDefinitions {

    val weapons = listOf(
        WeaponDef(
            id = "pulse_cannon",
            name = "Pulse Cannon",
            description = "Auto-targeting energy bolts",
            baseDamage = 11f,
            baseCooldown = 0.375f,
            baseProjectileSpeed = 600f,
            baseProjectileCount = 1,
            evolutionPassive = "duplicator_core",
            evolutionWeaponId = "storm_cannon",
            levelBonuses = listOf(
                "1 bolt",
                "+1 bolt",
                "+1 bolt",
                "+1 bolt",
                "+1 bolt"
            )
        ),
        WeaponDef(
            id = "energy_saw",
            name = "Energy Saw",
            description = "Spinning disc that shreds on contact",
            baseDamage = 8f,
            baseCooldown = 0.125f,
            baseProjectileSpeed = 0f,
            baseProjectileCount = 1,
            evolutionPassive = "momentum_drive",
            evolutionWeaponId = "warp_saw",
            levelBonuses = listOf(
                "1 blade",
                "Bigger blade",
                "Bigger blade",
                "Bigger blade",
                "Bigger blade, MAX"
            )
        ),
        WeaponDef(
            id = "scatter_shot",
            name = "Scatter Shot",
            description = "Wide pellet spread",
            baseDamage = 8f,
            baseCooldown = 0.75f,
            baseProjectileSpeed = 500f,
            baseProjectileCount = 5,
            evolutionPassive = "vampiric_core",
            evolutionWeaponId = "leech_burst",
            levelBonuses = listOf(
                "5 pellets",
                "+2 pellets",
                "+2 pellets",
                "+2 pellets",
                "+2 pellets"
            )
        ),
        WeaponDef(
            id = "homing_missiles",
            name = "Homing Missiles",
            description = "Lock-on missiles",
            baseDamage = 25f,
            baseCooldown = 1.0f,
            baseProjectileSpeed = 350f,
            baseProjectileCount = 1,
            evolutionPassive = "tb26",
            evolutionWeaponId = "autonomous_ace",
            levelBonuses = listOf(
                "1 missile",
                "+1 missile",
                "+1 missile",
                "+1 missile",
                "+1 missile"
            )
        ),
        WeaponDef(
            id = "ion_orbiters",
            name = "Ion Orbiters",
            description = "Orbiting energy spheres",
            baseDamage = 15f,
            baseCooldown = 3f,
            baseProjectileSpeed = 0f,
            baseProjectileCount = 2,
            evolutionPassive = "cryo_field",
            evolutionWeaponId = "frost_ring",
            levelBonuses = listOf(
                "2 orbiters",
                "+1 orbiter",
                "+1 orbiter",
                "+1 orbiter",
                "+1 orbiter"
            )
        ),
        WeaponDef(
            id = "railgun",
            name = "Railgun",
            description = "Piercing sniper shot",
            baseDamage = 80f,
            baseCooldown = 1.5f,
            baseProjectileSpeed = 2000f,
            baseProjectileCount = 1,
            evolutionPassive = "glass_cannon",
            evolutionWeaponId = "oblivion_beam",
            levelBonuses = listOf(
                "Pierces 10",
                "Wider shot",
                "Wider shot",
                "Wider shot",
                "Beam-wide, MAX"
            )
        ),
        WeaponDef(
            id = "space_mines",
            name = "Space Mines",
            description = "Dropped proximity mines",
            baseDamage = 60f,
            baseCooldown = 2f,
            baseProjectileSpeed = 0f,
            baseProjectileCount = 1,
            evolutionPassive = "lucky_star",
            evolutionWeaponId = "jackpot_mines",
            levelBonuses = listOf(
                "1 mine",
                "Bigger explosion",
                "+1 mine",
                "Bigger explosion",
                "+1 mine"
            )
        ),
        WeaponDef(
            id = "solar_storm",
            name = "Solar Storm",
            description = "Random lightning strikes",
            baseDamage = 29f,
            baseCooldown = 1.0f,
            baseProjectileSpeed = 0f,
            baseProjectileCount = 1,
            evolutionPassive = "phoenix_core",
            evolutionWeaponId = "phoenix_flare",
            levelBonuses = listOf(
                "1 target",
                "+1 target",
                "+1 target",
                "+1 target",
                "+1 target"
            )
        ),
        WeaponDef(
            id = "nova_blast",
            name = "Nova Blast",
            description = "Periodic AoE ring burst",
            baseDamage = 40f,
            baseCooldown = 4f,
            baseProjectileSpeed = 0f,
            baseProjectileCount = 1,
            evolutionPassive = "revenge_protocol",
            evolutionWeaponId = "lingering_nova",
            levelBonuses = listOf(
                "Base burst",
                "Bigger radius",
                "Bigger radius",
                "Bigger radius",
                "Bigger radius"
            )
        ),
        WeaponDef(
            id = "needle_gun",
            name = "Needle Gun",
            description = "Rapid piercing needles",
            baseDamage = 5f,
            baseCooldown = 0.25f,
            baseProjectileSpeed = 800f,
            baseProjectileCount = 3,
            evolutionPassive = "nano_repair",
            evolutionWeaponId = "siphon_needles",
            levelBonuses = listOf(
                "3 needles",
                "+1 needle",
                "+1 needle",
                "+1 needle",
                "+1 needle"
            )
        ),
        WeaponDef(
            id = "cluster_bomb",
            name = "Cluster Bomb",
            description = "Bomb that scatters bomblets",
            baseDamage = 60f,
            baseCooldown = 2f,
            baseProjectileSpeed = 200f,
            baseProjectileCount = 1,
            evolutionPassive = "magnet_field",
            evolutionWeaponId = "hunter_killer",
            levelBonuses = listOf(
                "2 bomblets",
                "+1 bomblet",
                "+1 bomblet",
                "+1 bomblet",
                "+1 bomblet"
            )
        ),
        WeaponDef(
            id = "flak_cannon",
            name = "Flak Cannon",
            description = "Direct-fire exploding shells",
            baseDamage = 33f,
            baseCooldown = 0.75f,
            baseProjectileSpeed = 400f,
            baseProjectileCount = 1,
            evolutionPassive = "extra_weapon_slot",
            evolutionWeaponId = "flak_barrage",
            levelBonuses = listOf(
                "1 shell",
                "+1 shell",
                "+1 shell",
                "+1 shell",
                "+1 shell"
            )
        )
    )

    // Evolution weapons (unlocked by combining weapon + passive)
    val evolutions = listOf(
        WeaponDef(
            id = "storm_cannon",
            name = "Storm Cannon",
            description = "Pulse Cannon + Duplicator Core: rapid multi-bolt storm",
            baseDamage = 18f,
            baseCooldown = 0.25f,
            baseProjectileSpeed = 700f,
            baseProjectileCount = 3
        ),
        WeaponDef(
            id = "warp_saw",
            name = "Warp Saw",
            description = "Energy Saw + Momentum Drive: the blade detaches and hunts on its own",
            baseDamage = 15f,
            baseCooldown = 0.125f,
            baseProjectileSpeed = 0f,
            baseProjectileCount = 1
        ),
        WeaponDef(
            id = "leech_burst",
            name = "Leech Burst",
            description = "Scatter Shot + Vampiric Core: healing pellets",
            baseDamage = 10f,
            baseCooldown = 0.5f,
            baseProjectileSpeed = 550f,
            baseProjectileCount = 13
        ),
        WeaponDef(
            id = "autonomous_ace",
            name = "Autonomous Ace",
            description = "Homing Missiles + Combat Drone: supercharged drone AI",
            baseDamage = 42f,
            baseCooldown = 0.75f,
            baseProjectileSpeed = 500f,
            baseProjectileCount = 5
        ),
        WeaponDef(
            id = "frost_ring",
            name = "Frost Ring",
            description = "Ion Orbiters + Cryo Field: slowing orbital ring",
            baseDamage = 20f,
            baseCooldown = 2f,
            baseProjectileSpeed = 0f,
            baseProjectileCount = 6
        ),
        WeaponDef(
            id = "oblivion_beam",
            name = "Oblivion Beam",
            description = "Railgun + Glass Cannon: always-on piercing lance",
            baseDamage = 18f,
            baseCooldown = 1.0f,
            baseProjectileSpeed = 3000f,
            baseProjectileCount = 1
        ),
        WeaponDef(
            id = "jackpot_mines",
            name = "Gambler's Mines",
            description = "Space Mines + Lucky Star: mines with random effects",
            baseDamage = 90f,
            baseCooldown = 1.5f,
            baseProjectileSpeed = 0f,
            baseProjectileCount = 3
        ),
        WeaponDef(
            id = "phoenix_flare",
            name = "Phoenix Flare",
            description = "Solar Storm + Phoenix Core: pulse rings erupt at enemy positions",
            baseDamage = 47f,
            baseCooldown = 0.75f,
            baseProjectileSpeed = 0f,
            baseProjectileCount = 1
        ),
        WeaponDef(
            id = "lingering_nova",
            name = "Lingering Nova",
            description = "Nova Blast + Revenge Protocol: detonates twice on the same spot",
            baseDamage = 30f,
            baseCooldown = 3f,
            baseProjectileSpeed = 0f,
            baseProjectileCount = 1
        ),
        WeaponDef(
            id = "siphon_needles",
            name = "Siphon Needles",
            description = "Needle Gun + Nano Repair: healing rapid fire",
            baseDamage = 9f,
            baseCooldown = 0.125f,
            baseProjectileSpeed = 900f,
            baseProjectileCount = 7
        ),
        WeaponDef(
            id = "hunter_killer",
            name = "Hunter-Killer",
            description = "Cluster Bomb + Magnet Field: relentless homing torpedo, double rate",
            baseDamage = 60f,
            baseCooldown = 1.0f,
            baseProjectileSpeed = 200f,
            baseProjectileCount = 1
        ),
        WeaponDef(
            id = "flak_barrage",
            name = "Flak Barrage",
            description = "Flak Cannon + Extra Weapon Slot: rapid cluster fire",
            baseDamage = 25f,
            baseCooldown = 0.5f,
            baseProjectileSpeed = 450f,
            baseProjectileCount = 5
        )
    )

    fun getWeaponDef(id: String): WeaponDef? {
        return weapons.find { it.id == id } ?: evolutions.find { it.id == id }
    }

    fun getBaseWeapons(): List<WeaponDef> = weapons

    fun getEvolutionFor(weaponId: String, passiveId: String): WeaponDef? {
        val weapon = weapons.find { it.id == weaponId } ?: return null
        if (weapon.evolutionPassive == passiveId) {
            return evolutions.find { it.id == weapon.evolutionWeaponId }
        }
        return null
    }

    fun isEvolution(id: String): Boolean = evolutions.any { it.id == id }

    fun getWeaponDisplayName(weaponId: String): String {
        return getWeaponDef(weaponId)?.name ?: weaponId
    }

    fun getEvolutionDisplayName(evolutionId: String, activePilotId: String): String {
        if (evolutionId == "autonomous_ace") {
            return if (activePilotId == "pilot_astro") "TB-26-X" else "Autonomous Ace"
        }
        return getWeaponDef(evolutionId)?.name ?: evolutionId
    }

    /**
     * Pilot-aware weapon icon id. Astro's Autonomous Ace (TB-26-X) uses its own icon, but
     * only outside Astro-Loop runs — there the drone is the generic green one — mirroring the
     * TB-26 drone rule in PassiveDefinitions.getDroneColor().
     */
    fun getWeaponIconId(weaponId: String, activePilotId: String, isAstroLoop: Boolean = false): String {
        if (weaponId == "autonomous_ace" && activePilotId == "pilot_astro" && !isAstroLoop) return "tb26_x"
        return weaponId
    }
}

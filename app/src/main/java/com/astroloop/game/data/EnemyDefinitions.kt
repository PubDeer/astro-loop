package com.astroloop.game.data

enum class EnemyType {
    SCOUT, TRACER, SHRAPNEL,         // Tier 1: Fodder (0-1 min)
    SENTINEL, TEMPEST, TRAP,          // Tier 2: Threat (2-4 min)
    DEVASTATOR, HEDGEHOG, QUASAR,     // Tier 3: Danger (5-6 min)
    DREADNOUGHT, RIPPER, SPECTER      // Tier 4: Elite (7-8 min)
}

data class EnemyDef(
    val type: EnemyType,
    val name: String,           // Callsign
    val shipId: String,         // Maps to ShipDefinitions
    val tier: Int,              // 1-4
    val unlockTime: Float,      // Seconds until this enemy can spawn
    val baseHealth: Float,
    val baseSpeed: Float,
    val baseDamage: Float,
    val fireRate: Float,        // Cooldown in seconds
    val preferredRange: Float,
    val upgradeDropCount: Int
)

object EnemyDefinitions {

    val enemies = listOf(
        // ── Tier 1: Fodder (0-60s) ──────────────────────────────────
        EnemyDef(
            type = EnemyType.SCOUT,
            name = "The Juke",
            shipId = "ship_blue",
            tier = 1,
            unlockTime = 0f,
            baseHealth = 25f,
            baseSpeed = 250f,
            baseDamage = 6f,
            fireRate = 2.0f,
            preferredRange = 250f,
            upgradeDropCount = 1
        ),
        EnemyDef(
            type = EnemyType.TRACER,
            name = "The Hunter",
            shipId = "ship_green",
            tier = 1,
            unlockTime = 30f,
            baseHealth = 30f,
            baseSpeed = 230f,
            baseDamage = 8f,
            fireRate = 1.8f,
            preferredRange = 280f,
            upgradeDropCount = 1
        ),
        EnemyDef(
            type = EnemyType.SHRAPNEL,
            name = "The Shotgunner",
            shipId = "ship_orange",
            tier = 1,
            unlockTime = 60f,
            baseHealth = 40f,
            baseSpeed = 180f,
            baseDamage = 10f,
            fireRate = 2.2f,
            preferredRange = 150f,
            upgradeDropCount = 1
        ),

        // ── Tier 2: Threat (120-240s) ───────────────────────────────
        EnemyDef(
            type = EnemyType.SENTINEL,
            name = "The Guardian",
            shipId = "ship_cyan",
            tier = 2,
            unlockTime = 120f,
            baseHealth = 60f,
            baseSpeed = 140f,
            baseDamage = 10f,
            fireRate = 1.5f,
            preferredRange = 180f,
            upgradeDropCount = 1
        ),
        EnemyDef(
            type = EnemyType.TEMPEST,
            name = "The Scorcher",
            shipId = "ship_coral",
            tier = 2,
            unlockTime = 180f,
            baseHealth = 50f,
            baseSpeed = 200f,
            baseDamage = 12f,
            fireRate = 1.2f,
            preferredRange = 220f,
            upgradeDropCount = 1
        ),
        EnemyDef(
            type = EnemyType.TRAP,
            name = "The Weaver",
            shipId = "ship_yellow",
            tier = 2,
            unlockTime = 240f,
            baseHealth = 70f,
            baseSpeed = 150f,
            baseDamage = 15f,
            fireRate = 2.0f,
            preferredRange = 300f,
            upgradeDropCount = 1
        ),

        // ── Tier 3: Danger (300-360s) ───────────────────────────────
        EnemyDef(
            type = EnemyType.DEVASTATOR,
            name = "The Flakker",
            shipId = "ship_lime",
            tier = 3,
            unlockTime = 300f,
            baseHealth = 100f,
            baseSpeed = 150f,
            baseDamage = 15f,
            fireRate = 1.8f,
            preferredRange = 200f,
            upgradeDropCount = 1
        ),
        EnemyDef(
            type = EnemyType.HEDGEHOG,
            name = "The Stitcher",
            shipId = "ship_purple",
            tier = 3,
            unlockTime = 330f,
            baseHealth = 80f,
            baseSpeed = 180f,
            baseDamage = 12f,
            fireRate = 0.8f,
            preferredRange = 250f,
            upgradeDropCount = 1
        ),
        EnemyDef(
            type = EnemyType.QUASAR,
            name = "The Bomber",
            shipId = "ship_red",
            tier = 3,
            unlockTime = 360f,
            baseHealth = 120f,
            baseSpeed = 130f,
            baseDamage = 20f,
            fireRate = 2.5f,
            preferredRange = 160f,
            upgradeDropCount = 1
        ),

        // ── Tier 4: Elite (420-480s) ────────────────────────────────
        EnemyDef(
            type = EnemyType.DREADNOUGHT,
            name = "The Juggernaut",
            shipId = "ship_indigo",
            tier = 4,
            unlockTime = 420f,
            baseHealth = 200f,
            baseSpeed = 100f,
            baseDamage = 25f,
            fireRate = 2.0f,
            preferredRange = 180f,
            upgradeDropCount = 1
        ),
        EnemyDef(
            type = EnemyType.RIPPER,
            name = "The Rusher",
            shipId = "ship_magenta",
            tier = 4,
            unlockTime = 450f,
            baseHealth = 100f,
            baseSpeed = 250f,
            baseDamage = 30f,
            fireRate = 0.6f,
            preferredRange = 80f,
            upgradeDropCount = 1
        ),
        EnemyDef(
            type = EnemyType.SPECTER,
            name = "The Phantom",
            shipId = "ship_white",
            tier = 4,
            unlockTime = 480f,
            baseHealth = 120f,
            baseSpeed = 200f,
            baseDamage = 20f,
            fireRate = 1.5f,
            preferredRange = 350f,
            upgradeDropCount = 1
        )
    )

    private val defsByType = enemies.associateBy { it.type }

    fun getDef(type: EnemyType): EnemyDef {
        return defsByType[type] ?: error("No EnemyDef for type $type")
    }

    fun getAvailableTypes(survivalTime: Float): List<EnemyType> {
        return enemies
            .filter { it.unlockTime <= survivalTime }
            .map { it.type }
    }

    fun getTier(type: EnemyType): Int {
        return getDef(type).tier
    }

    fun getTypesForTier(tier: Int): List<EnemyType> {
        return enemies
            .filter { it.tier == tier }
            .map { it.type }
    }

    fun getRandomType(survivalTime: Float): EnemyType {
        val available = getAvailableTypes(survivalTime)
        if (available.isEmpty()) return EnemyType.SCOUT

        // Weight toward earlier types (more common)
        val weights = available.mapIndexed { index, _ ->
            (available.size - index).toFloat()
        }
        val totalWeight = weights.sum()

        var random = kotlin.random.Random.nextFloat() * totalWeight
        for ((index, weight) in weights.withIndex()) {
            random -= weight
            if (random <= 0) {
                return available[index]
            }
        }

        return available.last()
    }
}

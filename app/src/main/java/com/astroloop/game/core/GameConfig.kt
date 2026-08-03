package com.astroloop.game.core

object GameConfig {
    // Display
    const val TARGET_FPS = 120
    const val FRAME_TIME_MS = 1000L / TARGET_FPS

    // Design resolution — all logical coordinates target this size
    const val DESIGN_WIDTH = 960f
    const val DESIGN_HEIGHT = 2142f

    // Ship
    const val SHIP_BASE_SPEED = 300f
    const val SHIP_BASE_HEALTH = 50f
    const val SHIP_BASE_SHIELDS = 50f
    const val SHIELD_REGEN_RATE = 1.0f
    const val SHIELD_REGEN_DELAY = 2f
    const val SHIP_BASE_SIZE = 25f
    const val SHIP_ACCELERATION = 800f
    const val SHIP_DECELERATION = 400f
    const val SHIP_INVULNERABILITY_TIME = 1.5f

    // Joystick
    const val JOYSTICK_DEAD_ZONE = 20f
    const val JOYSTICK_MAX_RADIUS = 120f

    // Asteroids
    const val ASTEROID_BASE_SPEED = 80f
    const val ASTEROID_LARGE_SIZE = 50f
    const val ASTEROID_MEDIUM_SIZE = 30f
    const val ASTEROID_SMALL_SIZE = 15f
    const val ASTEROID_SPAWN_MARGIN = 100f
    const val ASTEROID_INITIAL_SPAWN_RATE = 2f // seconds between spawns
    const val ASTEROID_MIN_SPAWN_RATE = 0.3f

    // Power-ups
    const val POWERUP_DROP_CHANCE = 0.3f

    /**
     * Fraction of the nominal rate a run pays out before any shop upgrades.
     *
     * Finder's Fee and Scavenger Rig each climb from this floor to the full nominal rate over
     * five levels. The floor lives here, applied at the payout site, rather than being folded
     * into the upgrade multiplier — that way the multiplier can start at 1.0 and the shop's
     * "+20%" label describes a real 20% increase on the first purchase. Folded in, the multiplier
     * ran 0.5..1.0 in +0.10 steps, so a "+10%" label was measuring against a 1.0 the player never
     * sees, while the actual first purchase was +20%.
     *
     * Changing these changes the economy. The rebase that introduced them did not.
     */
    const val YEN_BASE_RATE = 0.5f
    const val SALVAGE_BASE_RATE = 0.5f
    const val POWERUP_SIZE = 20f
    const val POWERUP_MAGNET_BASE_RANGE = 80f
    const val POWERUP_COLLECT_RANGE = 30f
    const val POWERUP_PULL_SPEED = 400f

    // Phoenix Core shockwave
    const val PHOENIX_SHOCKWAVE_MAX_RADIUS = 700f
    const val PHOENIX_SHOCKWAVE_DURATION = 0.6f

    // Boss charge sequence (fleet arrival scene)
    const val BOSS_CHARGE_DURATION = 62f        // seconds from charge-start to full charge (full fleet scene, retimed rescue)
    const val BOSS_EMP_CHARGE_THRESHOLD = 0.45f // guard: EMP only fires when charge ≥ this
    const val BOSS_CHARGED_SHOT_DAMAGE = 10000f // fat railgun kill-shot — instakills non-Astro pilots

    // Boss EMP rush-in
    const val BOSS_RUSH_TRIGGER_DISTANCE = 300f // EMP fires when the rusher closes to this gap
    const val BOSS_RUSH_SPEED_FLOOR = 700f      // rush is never slower than this (px/s)
    const val BOSS_RUSH_MAX_CLOSE_TIME = 3f     // gap/this bounds arrival time from any distance
    const val BOSS_RUSH_EASE_DURATION = 0.5f    // ignition ease-in (s)
    const val BOSS_RUSH_BRAKE_DURATION = 0.3f   // hard-brake window at arrival (s)
    const val BOSS_RUSH_ARRIVAL_BEAT = 0.4f     // stillness between brake and EMP (s)

    // Difficulty scaling (per minute)
    const val DIFFICULTY_SPAWN_RATE_INCREASE = 0.15f
    const val DIFFICULTY_SPEED_INCREASE = 0.15f
    const val ASTEROID_MAX_SPEED_FACTOR = 7.0f    // hard ceiling on asteroid speed multiplier
    const val DIFFICULTY_HEALTH_INCREASE = 0.05f

    // Weapons
    const val WEAPON_MAX_LEVEL = 5
    const val PASSIVE_MAX_STACKS = 5

    // Upgrade slots
    const val MAX_WEAPON_SLOTS = 4
    const val MAX_PASSIVE_SLOTS = 4
    const val PASSIVE_SLOTS_WITH_EXTRA_WEAPON = 3  // When using extra weapon slot passive

    // Upgrade selection
    const val UPGRADE_CHOICES = 3

    // Starfield
    const val STARS_FAR_COUNT = 50
    const val STARS_MID_COUNT = 30
    const val STARS_NEAR_COUNT = 15
    const val STARS_FAR_SPEED_FACTOR = 0.2f
    const val STARS_MID_SPEED_FACTOR = 0.5f
    const val STARS_NEAR_SPEED_FACTOR = 1.0f

    // Colors (ARGB format)
    const val COLOR_SHIP = 0xFF00FF00.toInt()       // Green
    const val COLOR_ASTEROID = 0xFFFFFFFF.toInt()   // White
    const val COLOR_ASTEROID_ICE = 0xFF88CCFF.toInt()
    const val COLOR_ASTEROID_METAL = 0xFFAAAAAA.toInt()
    const val COLOR_ASTEROID_VOLATILE = 0xFFFF8844.toInt()
    const val COLOR_ASTEROID_MAGNETIC = 0xFFFF44FF.toInt()
    const val COLOR_ASTEROID_TRAIL = 0xFF44FF44.toInt()    // Green
    const val COLOR_PROJECTILE = 0xFFFFFF00.toInt() // Yellow
    const val COLOR_POWERUP = 0xFF00FFFF.toInt()    // Cyan
    const val COLOR_HUD = 0xFFFFFFFF.toInt()        // White
    const val COLOR_HEALTH_BAR = 0xFF00FF00.toInt()
    const val COLOR_HEALTH_BAR_BG = 0xFF333333.toInt()
    const val COLOR_BACKGROUND = 0xFF000011.toInt()
    const val COLOR_STAR_FAR = 0xFF444444.toInt()
    const val COLOR_STAR_MID = 0xFF888888.toInt()
    const val COLOR_STAR_NEAR = 0xFFCCCCCC.toInt()

    // Time thresholds for unlocking asteroid types (in seconds)
    const val UNLOCK_ICE_ASTEROIDS = 60f      // 1 minute
    const val UNLOCK_METAL_ASTEROIDS = 120f   // 2 minutes
    const val UNLOCK_VOLATILE_ASTEROIDS = 180f // 3 minutes
    const val UNLOCK_MAGNETIC_ASTEROIDS = 240f // 4 minutes
    const val UNLOCK_TRAIL_ASTEROIDS = 300f     // 5 minutes

    // Enemy ships
    const val ENEMY_SPAWN_INTERVAL = 120f  // 2 minutes between spawns
    const val ENEMY_SPAWN_DISTANCE = 600f  // Spawn outside camera view
    const val ENEMY_DESPAWN_DISTANCE = 2000f  // Despawn when too far

    // Camera/world bounds
    const val ENTITY_DESPAWN_DISTANCE = 1500f  // Distance from camera to despawn asteroids

    // Critical hits
    const val CRIT_CHANCE_PER_LEVEL = 0.05f   // 5% per Lucky Rounds level
    const val CRIT_DAMAGE_MULTIPLIER = 2.0f   // 2x damage on crit

    // Upgrade drop rate limits
    const val ASTEROID_UPGRADE_DROP_COOLDOWN = 35f       // 35 seconds between asteroid drops (was 25)
    const val ASTEROID_UPGRADE_EARLY_COOLDOWN = 12f      // 12 second cooldown while above baseline (was 8)

    // Early game upgrade drop scaling
    const val ASTEROID_UPGRADE_DROP_INITIAL = 0.10f      // 10% starting chance (was 15%)
    const val ASTEROID_UPGRADE_DROP_BASELINE = 0.02f     // 2% baseline chance (was 3%)
    const val ASTEROID_UPGRADE_DROP_DECREASE = 0.01f     // Keep at -1% per asteroid upgrade

    // Astro Loop mode: boosted asteroid drop rates (no enemy drops in this mode)
    const val ASTRO_LOOP_UPGRADE_DROP_COOLDOWN = 20f     // 20s baseline cooldown (vs 35s)
    const val ASTRO_LOOP_UPGRADE_EARLY_COOLDOWN = 14f    // 14s early cooldown (was 8s) — slower early game
    const val ASTRO_LOOP_UPGRADE_DROP_INITIAL = 0.10f    // 10% starting chance (was 18%) — slower early game
    const val ASTRO_LOOP_UPGRADE_DROP_BASELINE = 0.05f   // 5% baseline chance (vs 2%)

    fun formatYen(amount: Int): String {
        return when {
            amount >= 1_000_000 -> {
                if (amount % 1_000_000 == 0) "${amount / 1_000_000}M"
                else "${String.format("%.1f", amount / 1_000_000f)}M"
            }
            amount >= 100_000 -> {
                val k = amount / 1_000
                if (amount % 1_000 == 0) "${k}K"
                else "${String.format("%.1f", amount / 1_000f)}K"
            }
            amount >= 10_000 -> {
                val k = amount / 1_000
                if (amount % 1_000 == 0) "¥${k}K"
                else "¥${String.format("%.1f", amount / 1_000f)}K"
            }
            else -> "¥$amount"
        }
    }
}

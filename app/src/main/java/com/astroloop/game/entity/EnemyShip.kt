package com.astroloop.game.entity

import com.astroloop.game.core.GameConfig
import com.astroloop.game.data.EnemyDefinitions
import com.astroloop.game.data.EnemyType
import com.astroloop.game.data.ShipDefinitions
import com.astroloop.game.util.Vector2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class AIState {
    APPROACH,       // Move toward player
    CIRCLE,         // Maintain distance while strafing
    ATTACK,         // Fire at player
    RETREAT,        // Flee when low HP
    DASH,           // Scout dash maneuver
    CLOAK           // Specter cloaked state
}

class EnemyShip : Entity(), Firer {
    override val isEnemyFirer: Boolean = true

    var type: EnemyType = EnemyType.SCOUT
    var aiState: AIState = AIState.APPROACH

    // Combat stats
    var fireCooldown: Float = 0f
    var fireRate: Float = 1.5f
    var weaponDamage: Float = 10f

    // AI parameters
    var preferredDistance: Float = 200f  // Distance to maintain from player
    var speed: Float = 150f
    var aggroRange: Float = 800f

    // Upgrades this enemy carries (dropped on death)
    var dropUpgrades: MutableList<String> = mutableListOf()

    // Visual
    var color: Int = 0xFFFF2233.toInt()

    // Scaling stats based on player upgrades
    var damageResistance: Float = 0f  // Reduces incoming damage (0 to 0.4 = 40%)
    var speedMultiplier: Float = 1f   // Multiplies base speed
    var frozenTimer: Float = 0f       // Seconds remaining on FROZEN slow effect
    var frozenBaseSpeed: Float = 1f   // speedMultiplier saved at freeze time; restored when FROZEN expires


    // Corrupted crew identity
    var shipId: String = ""           // Maps to ShipDefinitions for rendering
    var tier: Int = 1
    var isCrewmate: Boolean = false   // True for crewmate encounters — skip normal death/drop logic

    // Perfect dodge (past Astro in corruption run) — 100% evasion with visual sidestep
    var perfectDodge: Boolean = false
    var dodgeTimer: Float = 0f
    var dodgeOffsetX: Float = 0f
    var dodgeOffsetY: Float = 0f
    private val dodgeDuration = 0.4f   // Longer, smoother dodge (was 0.15f)
    private val dodgeDistance = 40f     // Slightly wider arc (was 30f)

    // Weapon system
    var weaponId: String = ""
    var weaponCooldown: Float = 0f
    var weaponFireRate: Float = 1.5f  // Weapon's base cooldown

    // Ion Orbiter state (Sentinel enemies)
    var orbiterProjectiles: MutableList<Projectile> = mutableListOf()
    var orbiterBaseAngle: Float = 0f
    var orbiterOrbitSpeed: Float = 3f

    // Energy Saw state (Ripper enemies)
    var sawDiscCount: Int = 1
    var sawReach: Float = 80f
    var sawDiscRadius: Float = 20f

    // The Juke (Scout) - dash behavior
    var dashCooldown: Float = 0f
    var isDashing: Boolean = false
    var dashTimer: Float = 0f

    // The Hunter (Tracer) - kiting
    var volleyCooldown: Float = 0f

    // The Phantom (Specter) - cloak
    var isCloaked: Boolean = false
    var cloakAlpha: Float = 1f
    var chargeUpTimer: Float = 0f
    var isChargingShot: Boolean = false
    var targetLineX: Float = 0f
    var targetLineY: Float = 0f
    var repositionTargetX: Float = 0f  // Specter reposition target during cloak
    var repositionTargetY: Float = 0f

    // The Juggernaut (Dreadnought) - rage speed boost
    var rageSpeedBonus: Float = 0f

    // Solar Storm telegraph
    var solarTelegraphActive: Boolean = false
    var solarTelegraphTimer: Float = 0f
    var solarTelegraphX: Float = 0f
    var solarTelegraphY: Float = 0f

    // State tracking
    var stateTimer: Float = 0f
    var circleDirection: Float = 1f  // 1 or -1 for clockwise/counterclockwise

    // Warp-in effect
    var spawnTime: Float = 0f
    val warpInDuration: Float = 1f
    val isWarping: Boolean get() = spawnTime < warpInDuration
    var warpInComplete: Boolean = false  // Flag set when warp-in just finished (for collision check)

    // Spawn invulnerability shield
    var spawnShieldDuration: Float = 5f  // Total duration (set by spawn system)
    var spawnShieldTimer: Float = 5f
    val isSpawnShielded: Boolean get() = spawnShieldTimer > 0f

    init {
        radius = GameConfig.SHIP_BASE_SIZE
        maxHealth = 50f
        health = maxHealth
    }

    fun initialize(
        x: Float,
        y: Float,
        enemyType: EnemyType,
        upgrades: List<String>
    ) {
        position.set(x, y)
        velocity.zero()
        type = enemyType
        aiState = AIState.APPROACH
        dropUpgrades.clear()
        dropUpgrades.addAll(upgrades)

        // Configure from EnemyDefinitions
        val def = EnemyDefinitions.getDef(type)
        shipId = def.shipId
        tier = def.tier
        maxHealth = def.baseHealth
        speed = def.baseSpeed
        weaponDamage = def.baseDamage
        fireRate = def.fireRate
        preferredDistance = def.preferredRange
        radius = GameConfig.SHIP_BASE_SIZE
        color = 0xFFFF2233.toInt()  // Bright red for all enemy types

        // Look up weapon from ship definition
        val shipDef = ShipDefinitions.getShip(shipId)
        if (shipDef != null) {
            weaponId = shipDef.startingWeaponId
        }
        weaponCooldown = 0f
        orbiterProjectiles.clear()
        orbiterBaseAngle = 0f

        health = maxHealth
        isActive = true
        fireCooldown = 0f
        stateTimer = 0f
        spawnTime = 0f
        spawnShieldTimer = spawnShieldDuration
        circleDirection = if (kotlin.random.Random.nextBoolean()) 1f else -1f
        // Reset scaling stats (will be set by spawn system)
        damageResistance = 0f
        speedMultiplier = 1f
        frozenTimer = 0f
        frozenBaseSpeed = 1f
        warpInComplete = false

        // Reset all type-specific fields
        solarTelegraphActive = false
        solarTelegraphTimer = 0f
        dashCooldown = 0f
        isDashing = false
        dashTimer = 0f
        volleyCooldown = 0f
        isCloaked = false
        cloakAlpha = 1f
        chargeUpTimer = 0f
        isChargingShot = false
        targetLineX = 0f
        targetLineY = 0f
        repositionTargetX = 0f
        repositionTargetY = 0f
        rageSpeedBonus = 0f

        // Type-specific initial state
        when (type) {
            EnemyType.SPECTER -> {
                isCloaked = true
                cloakAlpha = 0f
                aiState = AIState.CLOAK
            }
            else -> { }
        }
    }

    override fun update(deltaTime: Float) {
        // Track if warp was active before update
        val wasWarping = isWarping

        // Update spawn time for warp effect
        spawnTime += deltaTime

        // Check if warp-in just completed this frame
        warpInComplete = wasWarping && !isWarping

        // Update cooldowns
        if (fireCooldown > 0) {
            fireCooldown -= deltaTime
        }
        stateTimer += deltaTime

        // Count down spawn shield
        if (spawnShieldTimer > 0f) {
            spawnShieldTimer -= deltaTime
        }

        // Don't move while warping in
        if (!isWarping) {
            // Apply velocity
            super.update(deltaTime)
        }
    }

    fun canFire(): Boolean = fireCooldown <= 0

    fun resetFireCooldown() {
        fireCooldown = fireRate
    }

    fun updateAI(playerPos: Vector2, deltaTime: Float, meteorDodge: Vector2 = Vector2()) {
        val toPlayer = playerPos - position
        val distToPlayer = toPlayer.length()

        // State transitions based on distance and health
        when (aiState) {
            AIState.APPROACH -> {
                if (distToPlayer < preferredDistance) {
                    aiState = AIState.CIRCLE
                    stateTimer = 0f
                }
            }
            AIState.CIRCLE -> {
                // Occasionally switch direction
                if (stateTimer > 3f && kotlin.random.Random.nextFloat() < 0.02f) {
                    circleDirection *= -1f
                    stateTimer = 0f
                }
                // Attack if in range
                if (distToPlayer < aggroRange && canFire()) {
                    aiState = AIState.ATTACK
                }
                // Retreat if low HP (below 25%)
                if (health < maxHealth * 0.25f) {
                    aiState = AIState.RETREAT
                }
            }
            AIState.ATTACK -> {
                // Return to circle after attacking
                aiState = AIState.CIRCLE
            }
            AIState.RETREAT -> {
                // If health recovered or distance is safe, return to circle
                if (distToPlayer > aggroRange || health > maxHealth * 0.5f) {
                    aiState = AIState.CIRCLE
                }
            }
            // Type-specific states handled by EnemyAISystem
            AIState.DASH, AIState.CLOAK -> { }
        }

        // Movement based on state
        var targetVelocity = when (aiState) {
            AIState.APPROACH -> {
                // Move toward player
                if (distToPlayer > 0) {
                    toPlayer.normalized() * getEffectiveSpeed()
                } else {
                    Vector2()
                }
            }
            AIState.CIRCLE -> {
                // Circle around player at preferred distance
                if (distToPlayer > 0) {
                    val toPlayerNorm = toPlayer.normalized()
                    val perpendicular = toPlayerNorm.perpendicular() * circleDirection

                    // Adjust distance if too far or too close
                    val distanceAdjust = when {
                        distToPlayer > preferredDistance * 1.3f -> toPlayerNorm * 0.5f
                        distToPlayer < preferredDistance * 0.7f -> toPlayerNorm * -0.5f
                        else -> Vector2()
                    }

                    (perpendicular + distanceAdjust).normalized() * getEffectiveSpeed()
                } else {
                    Vector2()
                }
            }
            AIState.ATTACK -> {
                // Brief pause during attack
                velocity * 0.5f
            }
            AIState.RETREAT -> {
                // Move away from player
                if (distToPlayer > 0) {
                    toPlayer.normalized() * -getEffectiveSpeed()
                } else {
                    Vector2()
                }
            }
            // Type-specific states — movement handled by EnemyAISystem
            AIState.DASH, AIState.CLOAK -> {
                velocity  // Keep current velocity; EnemyAISystem sets it directly
            }
        }

        // Apply meteor dodge - this overrides normal movement when meteors are near
        if (meteorDodge.length() > 1f) {
            targetVelocity = targetVelocity + meteorDodge
            // Cap the velocity
            if (targetVelocity.length() > getEffectiveSpeed() * 1.5f) {
                targetVelocity = targetVelocity.normalized() * getEffectiveSpeed() * 1.5f
            }
        }

        // Smooth velocity change
        velocity.lerp(targetVelocity, 5f * deltaTime)

        // Face toward player (or away if retreating) — smooth rotation
        if (distToPlayer > 0) {
            val targetRotation = if (aiState == AIState.RETREAT) {
                (-toPlayer).angle()
            } else {
                toPlayer.angle()
            }
            // Smooth rotation — max turn rate prevents 180° snaps
            val maxTurnRate = 4f  // radians per second
            var diff = targetRotation - rotation
            // Wrap diff to [-PI, PI] for shortest path
            while (diff < -PI) diff += 2 * PI.toFloat()
            while (diff > PI) diff -= 2 * PI.toFloat()
            val maxTurn = maxTurnRate * deltaTime
            rotation += diff.coerceIn(-maxTurn, maxTurn)
        }
    }

    override fun takeDamage(amount: Float): Boolean {
        return takeDamage(amount, ignoreSpawnShield = false)
    }

    fun takeDamage(amount: Float, ignoreSpawnShield: Boolean): Boolean {
        // Perfect dodge — visual sidestep, no damage taken
        if (perfectDodge) {
            triggerDodge()
            return false
        }

        // Block all damage during spawn invulnerability (unless projectile ignores it)
        if (isSpawnShielded && !ignoreSpawnShield) return false

        // Apply damage resistance
        val reducedDamage = amount * (1f - damageResistance)
        return super.takeDamage(reducedDamage)
    }

    fun triggerDodge() {
        if (dodgeTimer > 0f) return // Already dodging
        dodgeTimer = dodgeDuration
        val perpAngle = rotation + PI.toFloat() / 2f * (if (Random.nextBoolean()) 1f else -1f)
        dodgeOffsetX = cos(perpAngle)  // Store unit direction, not full distance
        dodgeOffsetY = sin(perpAngle)
    }

    fun updateDodge(deltaTime: Float) {
        if (dodgeTimer > 0f) {
            dodgeTimer -= deltaTime
            if (dodgeTimer <= 0f) {
                dodgeTimer = 0f
                dodgeOffsetX = 0f
                dodgeOffsetY = 0f
            }
        }
    }

    /** Get current dodge offset — smooth sine curve instead of snap */
    fun getDodgeOffset(): Pair<Float, Float> {
        if (dodgeTimer <= 0f) return Pair(0f, 0f)
        val progress = 1f - (dodgeTimer / dodgeDuration)  // 0 → 1
        val curve = sin(progress * PI.toFloat())  // Smooth arc: 0 → 1 → 0
        return Pair(dodgeOffsetX * dodgeDistance * curve, dodgeOffsetY * dodgeDistance * curve)
    }

    // Get effective speed (base speed * multiplier)
    fun getEffectiveSpeed(): Float = speed * speedMultiplier

    override fun reset() {
        super.reset()
        type = EnemyType.SCOUT
        aiState = AIState.APPROACH
        fireCooldown = 0f
        stateTimer = 0f
        spawnTime = 0f
        dropUpgrades.clear()
        color = 0xFFFF2233.toInt()
        damageResistance = 0f
        speedMultiplier = 1f
        frozenTimer = 0f
        frozenBaseSpeed = 1f
        warpInComplete = false
        spawnShieldDuration = 5f
        spawnShieldTimer = 0f

        // Corrupted crew identity
        shipId = ""
        tier = 1
        isCrewmate = false
        perfectDodge = false
        dodgeTimer = 0f
        dodgeOffsetX = 0f
        dodgeOffsetY = 0f

        // Weapon system
        weaponId = ""
        weaponCooldown = 0f
        weaponFireRate = 1.5f
        orbiterProjectiles.clear()
        orbiterBaseAngle = 0f
        orbiterOrbitSpeed = 3f
        sawDiscCount = 1
        sawReach = 80f
        sawDiscRadius = 20f

        // The Juke (Scout)
        dashCooldown = 0f
        isDashing = false
        dashTimer = 0f

        // The Hunter (Tracer)
        volleyCooldown = 0f

        // The Phantom (Specter)
        isCloaked = false
        cloakAlpha = 1f
        chargeUpTimer = 0f
        isChargingShot = false
        targetLineX = 0f
        targetLineY = 0f
        repositionTargetX = 0f
        repositionTargetY = 0f

        // The Juggernaut (Dreadnought)
        rageSpeedBonus = 0f
    }
}

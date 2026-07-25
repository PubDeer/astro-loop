package com.astroloop.game.entity

import com.astroloop.game.core.GameConfig
import com.astroloop.game.util.Vector2
import kotlin.math.PI

class Ship : Entity(), Firer {
    override val isEnemyFirer: Boolean = false

    val moveDirection = Vector2()
    var speed: Float = GameConfig.SHIP_BASE_SPEED
    var invulnerabilityTimer: Float = 0f
    var isInvulnerable: Boolean = false
    var healthBarTimer: Float = 0f

    // Shield system
    var currentShield: Float = 0f
    var maxShield: Float = 0f
    var shieldRegenRate: Float = 0f
    var shieldRegenDelay: Float = 2f
    var timeSinceLastDamage: Float = 0f
    var shieldCap: Float = Float.MAX_VALUE  // Glass Cannon caps this
    var shieldRegenDisabled: Boolean = false  // Glass Cannon disables shield regen

    // Visual evolution tracking
    var evolutionStage: Int = 0
    var shipColor: Int = 0xFF3388FF.toInt()  // Default blue, set by ship selection
    var pilotColor: Int = 0xFFFFFFFF.toInt()
    var startingWeaponId: String = "pulse_cannon"
    var hasOrbiters: Boolean = false
    var hasMissiles: Boolean = false

    init {
        radius = GameConfig.SHIP_BASE_SIZE
        maxHealth = GameConfig.SHIP_BASE_HEALTH
        health = maxHealth
        maxShield = GameConfig.SHIP_BASE_SHIELDS
        currentShield = maxShield
        shieldRegenRate = GameConfig.SHIELD_REGEN_RATE
        shieldRegenDelay = GameConfig.SHIELD_REGEN_DELAY
        rotation = -PI.toFloat() / 2 // Point upward initially
    }

    override fun update(deltaTime: Float) {
        // Update invulnerability
        if (invulnerabilityTimer > 0) {
            invulnerabilityTimer -= deltaTime
            isInvulnerable = invulnerabilityTimer > 0
        }

        // Update health bar timer
        if (healthBarTimer > 0) {
            healthBarTimer -= deltaTime
        }

        // Shield regeneration (Glass Cannon disables this)
        if (shieldRegenDisabled) {
            currentShield = 0f
        } else {
            timeSinceLastDamage += deltaTime
            if (currentShield < maxShield && timeSinceLastDamage >= shieldRegenDelay) {
                currentShield = minOf(maxShield, currentShield + shieldRegenRate * deltaTime)
            }
            // Glass Cannon: enforce shield cap every frame
            currentShield = minOf(currentShield, shieldCap)
        }

        // Movement based on joystick direction
        if (!moveDirection.isZero()) {
            // moveDirection contains magnitude from touch controller (0-1)
            // Use magnitude to scale speed for variable ship speed
            val magnitude = moveDirection.length().coerceIn(0f, 1f)
            val direction = moveDirection.normalized()

            // Accelerate toward target velocity (speed scaled by thumb distance)
            val targetVelocity = direction * speed * magnitude
            velocity.lerp(targetVelocity, GameConfig.SHIP_ACCELERATION * deltaTime / speed)

            // Rotate ship to face movement direction
            val targetRotation = moveDirection.angle()
            rotation = lerpAngle(rotation, targetRotation, 8f * deltaTime)
        } else {
            // Decelerate
            val decel = GameConfig.SHIP_DECELERATION * deltaTime
            val currentSpeed = velocity.length()
            if (currentSpeed > decel) {
                velocity.mul((currentSpeed - decel) / currentSpeed)
            } else {
                velocity.zero()
            }
        }

        super.update(deltaTime)
    }

    override fun takeDamage(amount: Float): Boolean {
        if (isInvulnerable) return false

        // Reset regen timer on any damage
        timeSinceLastDamage = 0f
        healthBarTimer = 2f  // Show health bar for 2 seconds

        var remainingDamage = amount

        // Apply damage to shields first
        if (currentShield > 0f) {
            val shieldDamage = minOf(currentShield, remainingDamage)
            currentShield -= shieldDamage
            remainingDamage -= shieldDamage

        }

        // Bleed through to health
        if (remainingDamage > 0f) {
            health -= remainingDamage
            if (health <= 0f) {
                health = 0f
                isActive = false
                return true  // Ship destroyed
            }
        }

        return false
    }

    fun makeInvulnerable(duration: Float = GameConfig.SHIP_INVULNERABILITY_TIME) {
        invulnerabilityTimer = duration
        isInvulnerable = true
    }

    fun updateEvolutionStage(totalUpgrades: Int) {
        evolutionStage = when {
            totalUpgrades >= 19 -> 6
            totalUpgrades >= 16 -> 5
            totalUpgrades >= 13 -> 4
            totalUpgrades >= 10 -> 3
            totalUpgrades >= 7 -> 2
            totalUpgrades >= 4 -> 1
            else -> 0
        }
    }

    fun updateWeaponVisuals(weaponIds: Set<String>) {
        hasOrbiters = weaponIds.contains("ion_orbiters") || weaponIds.contains("frost_ring")
        hasMissiles = weaponIds.contains("homing_missiles") || weaponIds.contains("autonomous_ace")
    }

    override fun reset() {
        super.reset()
        moveDirection.zero()
        speed = GameConfig.SHIP_BASE_SPEED
        invulnerabilityTimer = 0f
        isInvulnerable = false
        healthBarTimer = 0f
        evolutionStage = 0
        shipColor = 0xFF3388FF.toInt()  // Reset to default blue
        pilotColor = 0xFFFFFFFF.toInt()
        startingWeaponId = "pulse_cannon"
        hasOrbiters = false
        hasMissiles = false
        maxHealth = GameConfig.SHIP_BASE_HEALTH
        health = maxHealth
        maxShield = GameConfig.SHIP_BASE_SHIELDS
        currentShield = maxShield
        shieldRegenRate = GameConfig.SHIELD_REGEN_RATE
        shieldRegenDelay = GameConfig.SHIELD_REGEN_DELAY
        timeSinceLastDamage = 0f
        shieldCap = Float.MAX_VALUE
        shieldRegenDisabled = false
        rotation = -PI.toFloat() / 2
    }

    /**
     * Refill shields to full. Clamps to shieldCap so Glass Cannon (cap 0) stays at 0.
     * Use this for revival/respawn — never assign currentShield = shieldCap, since
     * shieldCap defaults to Float.MAX_VALUE and would make the ship unkillable.
     */
    fun restoreShields() {
        currentShield = minOf(maxShield, shieldCap)
    }

    fun applyPermanentBonuses(healthBonus: Float, shieldsBonus: Float) {
        maxHealth = GameConfig.SHIP_BASE_HEALTH + healthBonus
        health = maxHealth
        maxShield = GameConfig.SHIP_BASE_SHIELDS + shieldsBonus
        currentShield = maxShield
    }

    private fun lerpAngle(from: Float, to: Float, t: Float): Float {
        var diff = to - from
        while (diff < -PI) diff += 2 * PI.toFloat()
        while (diff > PI) diff -= 2 * PI.toFloat()
        return from + diff * t.coerceIn(0f, 1f)
    }
}

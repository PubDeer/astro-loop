package com.astroloop.game.entity

import com.astroloop.game.core.GameConfig
import kotlin.math.sin
import kotlin.math.sqrt

enum class PowerUpType {
    WEAPON,
    PASSIVE,
    SCORE_PICKUP,       // Star dust (asteroids) or credits (enemies)
    EVOLUTION_DIAMOND   // Elite diamond from Tier 4 enemies — contains evolution
}

class PowerUp : Entity() {

    companion object {
        const val FADE_OUT_DURATION = 0.5f
    }

    var type: PowerUpType = PowerUpType.WEAPON
    var itemId: String = ""
    var pulsePhase: Float = 0f
    var isBeingPulled: Boolean = false
    var scoreValue: Int = 0  // For SCORE_PICKUP type
    var isFromEnemy: Boolean = false  // True = space mine bonus yen (cyan diamond), False = star dust (yellow)
    var fadeOutTimer: Float = -1f  // -1 = not fading; ≥0 = fading out

    init {
        radius = GameConfig.POWERUP_SIZE
    }

    override fun update(deltaTime: Float) {
        pulsePhase += deltaTime * 5f
        if (fadeOutTimer >= 0f) {
            fadeOutTimer -= deltaTime
            if (fadeOutTimer <= 0f) {
                isActive = false
            }
        }
        super.update(deltaTime)
    }

    fun initialize(x: Float, y: Float, powerUpType: PowerUpType, id: String) {
        position.set(x, y)
        velocity.zero()
        type = powerUpType
        itemId = id
        pulsePhase = 0f
        isActive = true
        isBeingPulled = false
        fadeOutTimer = -1f
    }

    fun getPulseScale(): Float {
        return 1f + sin(pulsePhase) * 0.15f
    }

    fun startFadeOut() {
        if (fadeOutTimer < 0f) fadeOutTimer = FADE_OUT_DURATION
    }

    fun getFadeAlpha(): Float {
        return if (fadeOutTimer >= 0f) (fadeOutTimer / FADE_OUT_DURATION).coerceIn(0f, 1f) else 1f
    }

    fun moveToward(targetX: Float, targetY: Float, speed: Float, deltaTime: Float) {
        val dx = targetX - position.x
        val dy = targetY - position.y
        val dist = sqrt(dx * dx + dy * dy)

        if (dist > 1f) {
            val moveSpeed = speed * deltaTime
            if (dist <= moveSpeed) {
                position.set(targetX, targetY)
            } else {
                position.x += (dx / dist) * moveSpeed
                position.y += (dy / dist) * moveSpeed
            }
        }
    }

    override fun reset() {
        super.reset()
        type = PowerUpType.WEAPON
        itemId = ""
        pulsePhase = 0f
        isBeingPulled = false
        scoreValue = 0
        isFromEnemy = false
        fadeOutTimer = -1f
        radius = GameConfig.POWERUP_SIZE
    }

    fun initializeAsEvolutionDiamond(x: Float, y: Float) {
        position.set(x, y)
        velocity.zero()
        type = PowerUpType.EVOLUTION_DIAMOND
        itemId = ""
        pulsePhase = 0f
        isActive = true
        isBeingPulled = false
        radius = GameConfig.POWERUP_SIZE
    }

    fun initializeAsScorePickup(x: Float, y: Float, score: Int, fromEnemy: Boolean) {
        position.set(x, y)
        velocity.zero()
        type = PowerUpType.SCORE_PICKUP
        scoreValue = score
        isFromEnemy = fromEnemy
        pulsePhase = 0f
        isActive = true
        isBeingPulled = false
        radius = if (fromEnemy) 12f else 8f  // Credits are larger than star dust
    }
}

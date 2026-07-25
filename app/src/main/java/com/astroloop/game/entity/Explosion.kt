package com.astroloop.game.entity

import com.astroloop.game.core.GameConfig
import com.astroloop.game.data.ShipDefinitions
import com.astroloop.game.util.Vector2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Represents a piece of debris from an explosion
 */
data class Debris(
    val position: Vector2,
    val velocity: Vector2,
    val rotation: Float,
    val rotationSpeed: Float,
    val size: Float,
    val color: Int,
    var lifetime: Float
)

/**
 * Explosion effect with debris particles
 */
class ShipExplosion {

    private val debris = mutableListOf<Debris>()
    var isActive: Boolean = false
    var timer: Float = 0f
    var duration: Float = 2f

    // Flash effect
    var flashIntensity: Float = 1f

    fun start(shipX: Float, shipY: Float, shipVx: Float, shipVy: Float, shipRotation: Float, shipSize: Float, shipColor: Int) {
        debris.clear()
        isActive = true
        timer = 0f
        flashIntensity = 1f

        val lighterColor = ShipDefinitions.lightenColor(shipColor)

        // Create debris pieces
        val debrisCount = 12
        for (i in 0 until debrisCount) {
            val angle = (i.toFloat() / debrisCount) * 2 * Math.PI.toFloat() + Random.nextFloat() * 0.5f
            val speed = 80f + Random.nextFloat() * 150f

            debris.add(Debris(
                position = Vector2(
                    shipX + cos(angle) * shipSize * 0.3f,
                    shipY + sin(angle) * shipSize * 0.3f
                ),
                velocity = Vector2(
                    shipVx * 0.3f + cos(angle) * speed,
                    shipVy * 0.3f + sin(angle) * speed
                ),
                rotation = Random.nextFloat() * 2 * Math.PI.toFloat(),
                rotationSpeed = (Random.nextFloat() - 0.5f) * 10f,
                size = shipSize * (0.15f + Random.nextFloat() * 0.25f),
                color = if (Random.nextFloat() < 0.3f) lighterColor else shipColor,
                lifetime = 1.5f + Random.nextFloat() * 0.5f
            ))
        }

        // Add some spark particles
        for (i in 0 until 8) {
            val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
            val speed = 150f + Random.nextFloat() * 200f

            debris.add(Debris(
                position = Vector2(shipX, shipY),
                velocity = Vector2(
                    cos(angle) * speed,
                    sin(angle) * speed
                ),
                rotation = 0f,
                rotationSpeed = 0f,
                size = 3f,
                color = 0xFFFFFF88.toInt(),
                lifetime = 0.3f + Random.nextFloat() * 0.3f
            ))
        }
    }

    fun update(deltaTime: Float) {
        if (!isActive) return

        timer += deltaTime
        flashIntensity = (1f - timer / 0.3f).coerceAtLeast(0f)

        // Update debris
        val iterator = debris.iterator()
        while (iterator.hasNext()) {
            val d = iterator.next()
            d.lifetime -= deltaTime

            if (d.lifetime <= 0) {
                iterator.remove()
            } else {
                // Update position
                d.position.add(d.velocity.x * deltaTime, d.velocity.y * deltaTime)

                // Apply drag
                d.velocity.mul(0.98f)

                // Update rotation
            }
        }

        // End explosion after duration
        if (timer >= duration) {
            isActive = false
        }
    }

    fun getDebris(): List<Debris> = debris

    fun getProgress(): Float = (timer / duration).coerceIn(0f, 1f)

    fun reset() {
        debris.clear()
        isActive = false
        timer = 0f
        flashIntensity = 0f
    }
}

/**
 * Visual effect for weapon explosions (flak, etc.)
 */
data class VisualEffect(
    var x: Float,
    var y: Float,
    var radius: Float,
    var maxRadius: Float,
    var color: Int,
    var age: Float = 0f,
    var lifetime: Float = 0.3f,
    var type: EffectType = EffectType.EXPLOSION
)

enum class EffectType {
    EXPLOSION,    // Expanding ring
    HIT_FLASH,    // Brief impact flash
    DAMAGE_NUMBER, // Floating damage text
    PHOENIX_SHOCKWAVE, // Phoenix Core death shockwave expanding ring
    FLAK_EXPLOSION, // Dissipating cloud — FlakDesigns design 12
    BOSS_SHOCKWAVE
}

/**
 * Floating damage number for visual feedback
 */
data class DamageNumber(
    var x: Float,
    var y: Float,
    var value: Int,
    var color: Int = 0xFFFFFFFF.toInt(),
    var isCrit: Boolean = false,
    var age: Float = 0f,
    var lifetime: Float = 1f,
    var label: String? = null
) {
    fun update(deltaTime: Float): Boolean {
        age += deltaTime
        y -= 50f * deltaTime  // Float upward
        return age < lifetime
    }

    fun getAlpha(): Float = (1f - age / lifetime).coerceIn(0f, 1f)
}

/**
 * Manager for visual effects
 */
class VisualEffectManager {
    private val effects = mutableListOf<VisualEffect>()
    private val damageNumbers = mutableListOf<DamageNumber>()

    fun addExplosion(x: Float, y: Float, radius: Float, color: Int = 0xFFFFAA44.toInt()) {
        effects.add(VisualEffect(
            x = x,
            y = y,
            radius = 0f,
            maxRadius = radius,
            color = color,
            lifetime = 0.4f,
            type = EffectType.EXPLOSION
        ))
    }

    fun addFlakExplosion(x: Float, y: Float, radius: Float, designIndex: Int = 12) {
        effects.add(VisualEffect(
            x = x,
            y = y,
            radius = 0f,
            maxRadius = radius,
            color = designIndex,  // repurposed to carry FlakDesigns index
            lifetime = 1.0f,
            type = EffectType.FLAK_EXPLOSION
        ))
    }

    fun addPhoenixShockwave(x: Float, y: Float) {
        effects.add(VisualEffect(
            x = x,
            y = y,
            radius = 0f,
            maxRadius = GameConfig.PHOENIX_SHOCKWAVE_MAX_RADIUS,
            color = 0xFFFFFF44.toInt(),
            lifetime = GameConfig.PHOENIX_SHOCKWAVE_DURATION,
            type = EffectType.PHOENIX_SHOCKWAVE
        ))
    }

    fun addBossShockwave(x: Float, y: Float) {
        effects.add(VisualEffect(
            x = x,
            y = y,
            radius = 0f,
            maxRadius = 600f,
            color = 0xFFFFFFFF.toInt(),
            lifetime = 0.6f,
            type = EffectType.BOSS_SHOCKWAVE
        ))
    }

    /** Big layered explosion for a major death — flash, three rings, and a shockwave. */
    fun addDeathBlast(x: Float, y: Float) {
        effects.add(VisualEffect(x = x, y = y, radius = 130f, maxRadius = 130f,
            color = 0xFFFFFFFF.toInt(), lifetime = 0.25f, type = EffectType.HIT_FLASH))
        effects.add(VisualEffect(x = x, y = y, radius = 0f, maxRadius = 90f,
            color = 0xFFFFEE88.toInt(), lifetime = 0.7f, type = EffectType.EXPLOSION))
        effects.add(VisualEffect(x = x, y = y, radius = 0f, maxRadius = 170f,
            color = 0xFFFFAA33.toInt(), lifetime = 0.9f, type = EffectType.EXPLOSION))
        effects.add(VisualEffect(x = x, y = y, radius = 0f, maxRadius = 250f,
            color = 0xFFFF6622.toInt(), lifetime = 1.1f, type = EffectType.EXPLOSION))
        effects.add(VisualEffect(x = x, y = y, radius = 0f, maxRadius = 380f,
            color = 0xFFFFCC66.toInt(), lifetime = 0.8f, type = EffectType.BOSS_SHOCKWAVE))
    }

    fun addHitFlash(x: Float, y: Float, radius: Float, color: Int = 0xFFFFFFFF.toInt()) {
        effects.add(VisualEffect(
            x = x,
            y = y,
            radius = radius,
            maxRadius = radius,
            color = color,
            lifetime = 0.1f,
            type = EffectType.HIT_FLASH
        ))
    }

    fun addDamageNumber(x: Float, y: Float, damage: Int, color: Int = 0xFFFFFFFF.toInt(), isCrit: Boolean = false, label: String? = null) {
        if (damageNumbers.size >= 50) damageNumbers.removeAt(0)
        damageNumbers.add(DamageNumber(
            x = x,
            y = y,
            value = damage,
            color = color,
            isCrit = isCrit,
            label = label
        ))
    }

    fun update(deltaTime: Float) {
        // Update effects
        val effectIterator = effects.iterator()
        while (effectIterator.hasNext()) {
            val effect = effectIterator.next()
            effect.age += deltaTime

            when (effect.type) {
                EffectType.EXPLOSION -> {
                    // Expand the ring
                    effect.radius = effect.maxRadius * (effect.age / effect.lifetime)
                }
                EffectType.HIT_FLASH -> {
                    // Shrink the flash
                    effect.radius = effect.maxRadius * (1f - effect.age / effect.lifetime)
                }
                EffectType.PHOENIX_SHOCKWAVE -> {
                    // Expand outward like explosion
                    effect.radius = effect.maxRadius * (effect.age / effect.lifetime)
                }
                EffectType.BOSS_SHOCKWAVE -> {
                    effect.radius = effect.maxRadius * (effect.age / effect.lifetime)
                }
                EffectType.FLAK_EXPLOSION -> {
                    // No radius update needed — FlakDesigns uses age/lifetime directly
                }
                else -> {}
            }

            if (effect.age >= effect.lifetime) {
                effectIterator.remove()
            }
        }

        // Update damage numbers
        damageNumbers.removeAll { !it.update(deltaTime) }
    }

    fun getEffects(): List<VisualEffect> = effects
    fun getDamageNumbers(): List<DamageNumber> = damageNumbers

    fun clear() {
        effects.clear()
        damageNumbers.clear()
    }
}

package com.astroloop.game.system

import com.astroloop.game.core.GameState
import com.astroloop.game.data.ShipDefinitions
import com.astroloop.game.entity.Asteroid
import com.astroloop.game.entity.EnemyShip
import com.astroloop.game.entity.Entity
import com.astroloop.game.entity.Ship
import com.astroloop.game.util.Geometry
import com.astroloop.game.weapon.weapons.OblivionBeam
import kotlin.math.cos
import kotlin.math.sin

/**
 * Oblivion Beam: permanent lance from the ship's nose in the facing direction.
 * Per-entity damage ticks (SawDamageSystem pattern), kill detonations, grind
 * sound only while cutting. Damages asteroids and enemies equally.
 */
class BeamDamageSystem(
    private val state: GameState,
    private val ship: Ship,
    private val weaponSystem: WeaponSystem,
    private val addDamageNumber: (x: Float, y: Float, amount: Int, color: Int, isCrit: Boolean) -> Unit,
    private val addHitFlash: (x: Float, y: Float, size: Float, color: Int) -> Unit,
    private val applyDamageModifiers: (Float) -> Pair<Float, Boolean>,
    private val onAsteroidDestroyed: (Asteroid) -> Unit,
    private val onEnemyDestroyed: (EnemyShip) -> Unit,
    private val playGrindSound: () -> Unit
) {
    companion object {
        const val BEAM_LENGTH = 1600f
        const val BEAM_HALF_WIDTH = 10f
        const val TICK_INTERVAL = 0.1f
        const val SOUND_COOLDOWN = 0.15f
    }

    var beamActive = false
        private set

    private val tickCooldowns = mutableMapOf<Entity, Float>()
    private var soundCooldown = 0f

    fun reset() {
        tickCooldowns.clear()
        soundCooldown = 0f
        beamActive = false
    }

    fun update(deltaTime: Float, enemies: List<EnemyShip>, asteroids: List<Asteroid>) {
        val weapon = weaponSystem.getWeapon("oblivion_beam") as? OblivionBeam
        beamActive = weapon != null
        if (weapon == null) {
            tickCooldowns.clear()
            return
        }

        if (soundCooldown > 0f) soundCooldown -= deltaTime
        val cooldownIter = tickCooldowns.iterator()
        while (cooldownIter.hasNext()) {
            val entry = cooldownIter.next()
            entry.setValue(entry.value - deltaTime)
            if (entry.value <= 0f) cooldownIter.remove()
        }

        val ax = ship.position.x + cos(ship.rotation) * ship.radius
        val ay = ship.position.y + sin(ship.rotation) * ship.radius
        val bx = ax + cos(ship.rotation) * BEAM_LENGTH
        val by = ay + sin(ship.rotation) * BEAM_LENGTH
        val color = ShipDefinitions.getEvolutionColor("railgun", state.isCorruptionRun)
        val damage = weapon.getDamage(state)
        var touchingEnemy = false

        for (enemy in enemies) {
            if (!enemy.isActive || enemy.isWarping) continue
            if (!enemy.isCrewmate && !isOnScreen(enemy)) continue  // bound work to the viewport, like the saw
            if (Geometry.distancePointToSegment(enemy.position.x, enemy.position.y, ax, ay, bx, by) >= enemy.radius + BEAM_HALF_WIDTH) continue
            touchingEnemy = true
            if (tickCooldowns.containsKey(enemy)) continue
            tickCooldowns[enemy] = TICK_INTERVAL
            val (dmg, isCrit) = applyDamageModifiers(damage)
            state.telemetryDamageByWeapon["oblivion_beam"] = (state.telemetryDamageByWeapon["oblivion_beam"] ?: 0f) + dmg
            state.telemetryTotalDamageDealt += dmg
            if (isCrit) { state.telemetryCritsThisMinute++; state.telemetryCritsTotal++ }
            val destroyed = enemy.takeDamage(dmg)
            if (!enemy.perfectDodge && !enemy.isSpawnShielded) {
                addDamageNumber(enemy.position.x, enemy.position.y - enemy.radius, dmg.toInt(), color, isCrit)
            }
            addHitFlash(enemy.position.x, enemy.position.y, 14f, color)
            if (destroyed) {
                onEnemyDestroyed(enemy)
            }
        }

        for (asteroid in asteroids) {
            if (!asteroid.isActive) continue
            if (!isOnScreen(asteroid)) continue  // bound work to the viewport, like the saw
            if (Geometry.distancePointToSegment(asteroid.position.x, asteroid.position.y, ax, ay, bx, by) >= asteroid.radius + BEAM_HALF_WIDTH) continue
            if (tickCooldowns.containsKey(asteroid)) continue
            tickCooldowns[asteroid] = TICK_INTERVAL
            val (dmg, isCrit) = applyDamageModifiers(damage)
            state.telemetryDamageByWeapon["oblivion_beam"] = (state.telemetryDamageByWeapon["oblivion_beam"] ?: 0f) + dmg
            state.telemetryTotalDamageDealt += dmg
            if (isCrit) { state.telemetryCritsThisMinute++; state.telemetryCritsTotal++ }
            val destroyed = asteroid.takeDamage(dmg)
            addDamageNumber(asteroid.position.x, asteroid.position.y - asteroid.radius, dmg.toInt(), color, isCrit)
            addHitFlash(asteroid.position.x, asteroid.position.y, 14f, color)
            if (destroyed) {
                onAsteroidDestroyed(asteroid)
            }
        }

        // Oblivion beam only sounds while cutting an enemy — silent on asteroids (by request).
        if (touchingEnemy && soundCooldown <= 0f) {
            playGrindSound()
            soundCooldown = SOUND_COOLDOWN
        }
    }

    private fun isOnScreen(entity: Entity): Boolean {
        val margin = 50f
        val cameraX = ship.position.x - state.screenWidth / 2f
        val cameraY = ship.position.y - state.screenHeight / 2f
        return entity.position.x >= cameraX - margin &&
               entity.position.x <= cameraX + state.screenWidth + margin &&
               entity.position.y >= cameraY - margin &&
               entity.position.y <= cameraY + state.screenHeight + margin
    }
}

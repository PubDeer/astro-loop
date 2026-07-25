package com.astroloop.game.system

import com.astroloop.game.entity.AIState
import com.astroloop.game.entity.Asteroid
import com.astroloop.game.entity.AsteroidType
import com.astroloop.game.entity.EnemyShip
import com.astroloop.game.data.EnemyType
import com.astroloop.game.entity.EntityPool
import com.astroloop.game.entity.Projectile
import com.astroloop.game.entity.ProjectileType
import com.astroloop.game.entity.Ship
import com.astroloop.game.util.Vector2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class EnemyAISystem(
    private val projectilePool: EntityPool<Projectile>
) {

    companion object {
        const val METEOR_DODGE_RANGE = 200f
        const val PROJECTILE_DODGE_RANGE = 80f
        const val MINE_DODGE_RANGE = 150f
        const val DODGE_STRENGTH = 300f
        const val MISSILE_INTERCEPT_RANGE = 120f
        const val MISSILE_INTERCEPT_CHANCE = 0.9f  // 90% chance to shoot down
    }

    private val playerProjectilesCache = ArrayList<Projectile>(100)

    fun update(
        enemies: List<EnemyShip>,
        ship: Ship,
        deltaTime: Float,
        asteroids: List<Asteroid> = emptyList(),
        projectiles: List<Projectile> = emptyList()
    ) {
        // Get player projectiles for dodging
        playerProjectilesCache.clear()
        for (p in projectiles) {
            if (p.isActive && !p.isEnemyProjectile) playerProjectilesCache.add(p)
        }
        val playerProjectiles = playerProjectilesCache

        for (enemy in enemies) {
            if (!enemy.isActive) continue

            // Crewmates own their position entirely (CrewmateEncounter + GSV autopilot).
            // Tick timers only — skip enemy.update() so we never apply their velocity to
            // position here. Velocity is set by crewmate systems purely for thruster rendering.
            if (enemy.isCrewmate) {
                enemy.spawnTime += deltaTime
                enemy.stateTimer += deltaTime
                if (enemy.fireCooldown > 0f) enemy.fireCooldown -= deltaTime
                if (enemy.spawnShieldTimer > 0f) enemy.spawnShieldTimer -= deltaTime
                continue
            }

            // Always tick update (advances spawnTime for warp-in, applies velocity)
            enemy.update(deltaTime)

            // Decay FROZEN slow from Gambler's Mines
            if (enemy.frozenTimer > 0f) {
                enemy.frozenTimer -= deltaTime
                if (enemy.frozenTimer <= 0f) {
                    enemy.speedMultiplier = enemy.frozenBaseSpeed
                }
            }

            // Skip AI while warping in
            if (enemy.isWarping) continue

            // Check for nearby meteors and apply dodge behavior
            val meteorDodge = calculateMeteorDodge(enemy, asteroids)

            // Check for nearby player projectiles and mines
            val projectileDodge = calculateProjectileDodge(enemy, playerProjectiles)

            // Check for nearby trail asteroid wakes
            val trailDodge = calculateTrailDodge(enemy, asteroids)

            // Combine dodge vectors
            val combinedDodge = Vector2(
                meteorDodge.x + projectileDodge.x + trailDodge.x,
                meteorDodge.y + projectileDodge.y + trailDodge.y
            )

            // Try to intercept incoming homing missiles
            tryInterceptMissile(enemy, playerProjectiles)

            // Per-type AI behavior (movement only)
            when (enemy.type) {
                EnemyType.SCOUT -> updateDogfighter(enemy, ship, combinedDodge, deltaTime)
                EnemyType.TRACER -> updateKiter(enemy, ship, combinedDodge, deltaTime)
                EnemyType.SHRAPNEL -> updateAmbusher(enemy, ship, combinedDodge, deltaTime)
                EnemyType.SENTINEL -> updateBrawler(enemy, ship, combinedDodge, deltaTime)
                EnemyType.DEVASTATOR -> updateBombardier(enemy, ship, combinedDodge, deltaTime)
                EnemyType.TRAP -> updateFlyby(enemy, ship, combinedDodge, deltaTime)
                EnemyType.DREADNOUGHT -> updateSiege(enemy, ship, combinedDodge, deltaTime)
                EnemyType.HEDGEHOG -> updateSuppressor(enemy, ship, combinedDodge, deltaTime)
                EnemyType.QUASAR -> updateDiver(enemy, ship, combinedDodge, deltaTime)
                EnemyType.TEMPEST -> updateWanderer(enemy, ship, combinedDodge, deltaTime)
                EnemyType.RIPPER -> updateAggressiveBrawler(enemy, ship, combinedDodge, deltaTime)
                EnemyType.SPECTER -> updateSniper(enemy, ship, combinedDodge, deltaTime)
            }
        }
    }

    // ── Scout — Dogfighter ────────────────────────────────────────────────────
    // Circles at 200f. Strafes perpendicular. Dash-away on 3s cooldown when
    // player within 100f (2x speed, 0.5s, reverses strafe direction).

    private fun updateDogfighter(enemy: EnemyShip, ship: Ship, dodge: Vector2, deltaTime: Float) {
        val toPlayer = ship.position - enemy.position
        val dist = toPlayer.length()

        // Update dash cooldown
        enemy.dashCooldown = (enemy.dashCooldown - deltaTime).coerceAtLeast(0f)

        // Handle dash state
        if (enemy.isDashing) {
            enemy.dashTimer -= deltaTime
            if (enemy.dashTimer <= 0f) {
                enemy.isDashing = false
                enemy.aiState = AIState.CIRCLE
                enemy.stateTimer = 0f
            }
            // During dash, just apply dodge and rotate — velocity already set
            applyDodge(enemy, dodge, deltaTime)
            rotateToward(enemy, toPlayer, deltaTime)
            return
        }

        // Trigger dash if player too close and cooldown ready
        if (dist < 100f && enemy.dashCooldown <= 0f) {
            enemy.isDashing = true
            enemy.dashTimer = 0.5f
            enemy.dashCooldown = 3f
            enemy.circleDirection *= -1f  // Reverse strafe direction
            enemy.aiState = AIState.DASH
            // Dash away from player with some randomness
            val awayDir = (enemy.position - ship.position)
            val awayAngle = if (awayDir.length() > 0f) awayDir.angle() else 0f
            val angle = awayAngle + (Random.nextFloat() - 0.5f) * 0.8f
            val dashSpeed = enemy.getEffectiveSpeed() * 2f
            enemy.velocity.set(cos(angle) * dashSpeed, sin(angle) * dashSpeed)
            return
        }

        // Movement: circle at ~200f, strafing perpendicular
        when (enemy.aiState) {
            AIState.APPROACH -> {
                if (dist < 200f) {
                    enemy.aiState = AIState.CIRCLE
                    enemy.stateTimer = 0f
                }
                if (dist > 0) {
                    val target = toPlayer.normalized() * enemy.getEffectiveSpeed()
                    enemy.velocity.lerp(target + dodge, 5f * deltaTime)
                }
            }
            AIState.CIRCLE -> {
                if (dist > 0) {
                    val toPlayerNorm = toPlayer.normalized()
                    val perp = toPlayerNorm.perpendicular() * enemy.circleDirection
                    val distAdjust = when {
                        dist > 260f -> toPlayerNorm * 0.5f
                        dist < 140f -> toPlayerNorm * -0.5f
                        else -> Vector2()
                    }
                    val target = (perp + distAdjust).normalized() * enemy.getEffectiveSpeed()
                    enemy.velocity.lerp(target + dodge, 5f * deltaTime)
                }
                // Occasionally switch strafe direction
                enemy.stateTimer += deltaTime
                if (enemy.stateTimer > 2f && Random.nextFloat() < 0.03f) {
                    enemy.circleDirection *= -1f
                    enemy.stateTimer = 0f
                }
            }
            else -> {
                enemy.aiState = AIState.APPROACH
                enemy.stateTimer = 0f
            }
        }

        rotateToward(enemy, toPlayer, deltaTime)
    }

    // ── Tracer — Kiter ────────────────────────────────────────────────────────
    // Kites at 1.5x preferred distance. Retreats at 1.5x speed if player closes
    // to preferred distance. Strafes at kite range with slight drift away.

    private fun updateKiter(enemy: EnemyShip, ship: Ship, dodge: Vector2, deltaTime: Float) {
        val toPlayer = ship.position - enemy.position
        val dist = toPlayer.length()
        val kiteDistance = enemy.preferredDistance * 1.5f

        when (enemy.aiState) {
            AIState.APPROACH -> {
                if (dist < kiteDistance) {
                    enemy.aiState = AIState.CIRCLE
                    enemy.stateTimer = 0f
                }
                if (dist > 0) {
                    val target = toPlayer.normalized() * enemy.getEffectiveSpeed()
                    enemy.velocity.lerp(target + dodge, 5f * deltaTime)
                }
            }
            AIState.CIRCLE -> {
                if (dist > 0) {
                    val toPlayerNorm = toPlayer.normalized()

                    val distAdjust = when {
                        dist < enemy.preferredDistance -> {
                            // Too close — retreat at 1.5x speed
                            enemy.aiState = AIState.RETREAT
                            enemy.stateTimer = 0f
                            toPlayerNorm * -1f
                        }
                        dist < kiteDistance -> {
                            // Slightly too close — drift away
                            toPlayerNorm * -0.4f
                        }
                        dist > kiteDistance * 1.3f -> {
                            // Too far — drift closer
                            toPlayerNorm * 0.4f
                        }
                        else -> Vector2()
                    }

                    if (enemy.aiState == AIState.CIRCLE) {
                        val perp = toPlayerNorm.perpendicular() * enemy.circleDirection * 0.3f
                        val target = (distAdjust + perp).normalized() * enemy.getEffectiveSpeed()
                        enemy.velocity.lerp(target + dodge, 5f * deltaTime)
                    }
                }

                enemy.stateTimer += deltaTime
                if (enemy.stateTimer > 3f && Random.nextFloat() < 0.02f) {
                    enemy.circleDirection *= -1f
                    enemy.stateTimer = 0f
                }
            }
            AIState.RETREAT -> {
                // Run away at 1.5x speed
                if (dist > 0) {
                    val awayDir = (enemy.position - ship.position).normalized()
                    val target = awayDir * enemy.getEffectiveSpeed() * 1.5f
                    enemy.velocity.lerp(target + dodge, 5f * deltaTime)
                }
                // Return to kiting once safe distance reached
                if (dist > kiteDistance) {
                    enemy.aiState = AIState.CIRCLE
                    enemy.stateTimer = 0f
                }
            }
            else -> {
                enemy.aiState = AIState.CIRCLE
                enemy.stateTimer = 0f
            }
        }

        rotateToward(enemy, toPlayer, deltaTime)
    }

    // ── Shrapnel — Ambusher ───────────────────────────────────────────────────
    // Two-state: APPROACH at 1.3x speed until within 120f, then RETREAT for 1.5s. Loops.

    private fun updateAmbusher(enemy: EnemyShip, ship: Ship, dodge: Vector2, deltaTime: Float) {
        val toPlayer = ship.position - enemy.position
        val dist = toPlayer.length()

        when (enemy.aiState) {
            AIState.APPROACH -> {
                // Aggressive approach at 1.3x speed
                if (dist > 0) {
                    val target = toPlayer.normalized() * enemy.getEffectiveSpeed() * 1.3f
                    enemy.velocity.lerp(target + dodge, 5f * deltaTime)
                }
                // Switch to retreat when close
                if (dist < 120f) {
                    enemy.aiState = AIState.RETREAT
                    enemy.stateTimer = 0f
                }
            }
            AIState.RETREAT -> {
                // Run away for 1.5 seconds
                enemy.stateTimer += deltaTime
                if (dist > 0) {
                    val awayDir = (enemy.position - ship.position).normalized()
                    val target = awayDir * enemy.getEffectiveSpeed()
                    enemy.velocity.lerp(target + dodge, 5f * deltaTime)
                }
                if (enemy.stateTimer > 1.5f) {
                    enemy.aiState = AIState.APPROACH
                    enemy.stateTimer = 0f
                }
            }
            else -> {
                enemy.aiState = AIState.APPROACH
                enemy.stateTimer = 0f
            }
        }

        rotateToward(enemy, toPlayer, deltaTime)
    }

    // ── Sentinel — Brawler ────────────────────────────────────────────────────
    // Approaches to 35f and stays. Slow (0.8x speed), tight circling. Never retreats.

    private fun updateBrawler(enemy: EnemyShip, ship: Ship, dodge: Vector2, deltaTime: Float) {
        val toPlayer = ship.position - enemy.position
        val dist = toPlayer.length()

        when (enemy.aiState) {
            AIState.APPROACH -> {
                if (dist < 35f) {
                    enemy.aiState = AIState.CIRCLE
                    enemy.stateTimer = 0f
                }
                if (dist > 0) {
                    val target = toPlayer.normalized() * enemy.getEffectiveSpeed() * 0.8f
                    enemy.velocity.lerp(target + dodge, 5f * deltaTime)
                }
            }
            AIState.CIRCLE -> {
                // Tight circling at close range, slow
                if (dist > 0) {
                    val toPlayerNorm = toPlayer.normalized()
                    val perp = toPlayerNorm.perpendicular() * enemy.circleDirection
                    val distAdjust = when {
                        dist > 120f -> toPlayerNorm * 0.5f
                        dist < 50f -> toPlayerNorm * -0.3f
                        else -> Vector2()
                    }
                    val target = (perp + distAdjust).normalized() * enemy.getEffectiveSpeed() * 0.8f
                    enemy.velocity.lerp(target + dodge, 5f * deltaTime)
                }
                enemy.stateTimer += deltaTime
                if (enemy.stateTimer > 3f && Random.nextFloat() < 0.02f) {
                    enemy.circleDirection *= -1f
                    enemy.stateTimer = 0f
                }
            }
            else -> {
                enemy.aiState = AIState.APPROACH
                enemy.stateTimer = 0f
            }
        }

        rotateToward(enemy, toPlayer, deltaTime)
    }

    // ── Devastator — Bombardier ───────────────────────────────────────────────
    // Mid-range (preferred distance), slow (0.7x speed), steady circling. Never retreats.

    private fun updateBombardier(enemy: EnemyShip, ship: Ship, dodge: Vector2, deltaTime: Float) {
        val toPlayer = ship.position - enemy.position
        val dist = toPlayer.length()

        when (enemy.aiState) {
            AIState.APPROACH -> {
                if (dist < enemy.preferredDistance) {
                    enemy.aiState = AIState.CIRCLE
                    enemy.stateTimer = 0f
                }
                if (dist > 0) {
                    val target = toPlayer.normalized() * enemy.getEffectiveSpeed() * 0.7f
                    enemy.velocity.lerp(target + dodge, 5f * deltaTime)
                }
            }
            AIState.CIRCLE -> {
                // Slow deliberate circling, holds ground
                if (dist > 0) {
                    val toPlayerNorm = toPlayer.normalized()
                    val perp = toPlayerNorm.perpendicular() * enemy.circleDirection
                    val distAdjust = when {
                        dist > enemy.preferredDistance * 1.3f -> toPlayerNorm * 0.5f
                        dist < enemy.preferredDistance * 0.7f -> toPlayerNorm * -0.3f
                        else -> Vector2()
                    }
                    val target = (perp + distAdjust).normalized() * enemy.getEffectiveSpeed() * 0.7f
                    enemy.velocity.lerp(target + dodge, 5f * deltaTime)
                }
                enemy.stateTimer += deltaTime
                if (enemy.stateTimer > 4f && Random.nextFloat() < 0.02f) {
                    enemy.circleDirection *= -1f
                    enemy.stateTimer = 0f
                }
            }
            else -> {
                enemy.aiState = AIState.APPROACH
                enemy.stateTimer = 0f
            }
        }

        rotateToward(enemy, toPlayer, deltaTime)
    }

    // ── Trap — Flyby ──────────────────────────────────────────────────────────
    // Two-state: CIRCLE wide at 1.3x speed (3s), then APPROACH to dash through
    // player's space. Reverses circle direction on exit.

    private fun updateFlyby(enemy: EnemyShip, ship: Ship, dodge: Vector2, deltaTime: Float) {
        val toPlayer = ship.position - enemy.position
        val dist = toPlayer.length()

        when (enemy.aiState) {
            AIState.CIRCLE -> {
                // Wide circling at 1.3x speed
                if (dist > 0) {
                    val toPlayerNorm = toPlayer.normalized()
                    val perp = toPlayerNorm.perpendicular() * enemy.circleDirection
                    val distAdjust = when {
                        dist > enemy.preferredDistance * 1.5f -> toPlayerNorm * 0.5f
                        dist < enemy.preferredDistance * 0.8f -> toPlayerNorm * -0.3f
                        else -> Vector2()
                    }
                    val target = (perp + distAdjust).normalized() * enemy.getEffectiveSpeed() * 1.3f
                    enemy.velocity.lerp(target + dodge, 5f * deltaTime)
                }

                enemy.stateTimer += deltaTime
                if (enemy.stateTimer > 3f) {
                    // Begin fly-through approach
                    enemy.aiState = AIState.APPROACH
                    enemy.stateTimer = 0f
                }
            }
            AIState.APPROACH -> {
                // Dash through player's space
                if (dist > 0) {
                    val target = toPlayer.normalized() * enemy.getEffectiveSpeed() * 1.3f
                    enemy.velocity.lerp(target + dodge, 5f * deltaTime)
                }

                // Once we've passed through (behind player), reverse and circle again
                if (dist < 60f) {
                    enemy.stateTimer += deltaTime
                }
                // After passing close or timing out, return to circling
                if (enemy.stateTimer > 0.5f || (enemy.stateTimer > 0f && dist > 150f)) {
                    enemy.aiState = AIState.CIRCLE
                    enemy.stateTimer = 0f
                    enemy.circleDirection *= -1f  // Reverse circle direction on exit
                }
            }
            else -> {
                enemy.aiState = AIState.CIRCLE
                enemy.stateTimer = 0f
            }
        }

        rotateToward(enemy, toPlayer, deltaTime)
    }

    // ── Dreadnought — Siege ───────────────────────────────────────────────────
    // Approaches to preferred distance and stops (0.1x speed). Rage speed:
    // +50% below 50% HP, +100% below 25% HP.

    private fun updateSiege(enemy: EnemyShip, ship: Ship, dodge: Vector2, deltaTime: Float) {
        val toPlayer = ship.position - enemy.position
        val dist = toPlayer.length()

        // Update rage based on HP
        val hpPercent = enemy.health / enemy.maxHealth
        enemy.rageSpeedBonus = when {
            hpPercent < 0.25f -> 1.0f   // Below 25%: double speed
            hpPercent < 0.50f -> 0.5f   // Below 50%: 50% faster
            else -> 0f
        }

        val effectiveSpeed = enemy.getEffectiveSpeed() * (1f + enemy.rageSpeedBonus)

        when (enemy.aiState) {
            AIState.APPROACH -> {
                if (dist < enemy.preferredDistance) {
                    enemy.aiState = AIState.CIRCLE
                    enemy.stateTimer = 0f
                }
                if (dist > 0) {
                    val target = toPlayer.normalized() * effectiveSpeed
                    enemy.velocity.lerp(target + dodge, 3f * deltaTime)
                }
            }
            AIState.CIRCLE -> {
                // Near-stationary at preferred distance, ponderous
                if (dist > 0) {
                    val toPlayerNorm = toPlayer.normalized()
                    val distAdjust = when {
                        dist > enemy.preferredDistance * 1.3f -> toPlayerNorm * 0.5f
                        dist < enemy.preferredDistance * 0.7f -> toPlayerNorm * -0.3f
                        else -> Vector2()
                    }
                    val target = distAdjust.normalized() * effectiveSpeed * 0.1f
                    enemy.velocity.lerp(target + dodge, 3f * deltaTime)
                }
            }
            else -> {
                enemy.aiState = AIState.APPROACH
                enemy.stateTimer = 0f
            }
        }

        rotateToward(enemy, toPlayer, deltaTime)
    }

    // ── Hedgehog — Suppressor ─────────────────────────────────────────────────
    // Strafes at preferred distance while firing. Changes strafe direction every
    // 2-3s. Backs off if too close.

    private fun updateSuppressor(enemy: EnemyShip, ship: Ship, dodge: Vector2, deltaTime: Float) {
        val toPlayer = ship.position - enemy.position
        val dist = toPlayer.length()

        when (enemy.aiState) {
            AIState.APPROACH -> {
                if (dist < enemy.preferredDistance) {
                    enemy.aiState = AIState.CIRCLE
                    enemy.stateTimer = 0f
                }
                if (dist > 0) {
                    val target = toPlayer.normalized() * enemy.getEffectiveSpeed()
                    enemy.velocity.lerp(target + dodge, 5f * deltaTime)
                }
            }
            AIState.CIRCLE -> {
                if (dist > 0) {
                    val toPlayerNorm = toPlayer.normalized()
                    val perp = toPlayerNorm.perpendicular() * enemy.circleDirection
                    val distAdjust = when {
                        dist > enemy.preferredDistance * 1.3f -> toPlayerNorm * 0.5f
                        dist < enemy.preferredDistance * 0.5f -> toPlayerNorm * -0.8f  // Back off hard
                        dist < enemy.preferredDistance * 0.7f -> toPlayerNorm * -0.5f
                        else -> Vector2()
                    }
                    val target = (perp + distAdjust).normalized() * enemy.getEffectiveSpeed()
                    enemy.velocity.lerp(target + dodge, 5f * deltaTime)
                }

                // Change strafe direction every 2-3s
                enemy.stateTimer += deltaTime
                if (enemy.stateTimer > 2f + Random.nextFloat() && Random.nextFloat() < 0.05f) {
                    enemy.circleDirection *= -1f
                    enemy.stateTimer = 0f
                }
            }
            else -> {
                enemy.aiState = AIState.APPROACH
                enemy.stateTimer = 0f
            }
        }

        rotateToward(enemy, toPlayer, deltaTime)
    }

    // ── Nova (Quasar) — Diver ─────────────────────────────────────────────────
    // Two-state: APPROACH at 1.5x speed until 60f (nova range), then RETREAT
    // until 300f or 3s. Telegraphed dive pattern.

    private fun updateDiver(enemy: EnemyShip, ship: Ship, dodge: Vector2, deltaTime: Float) {
        val toPlayer = ship.position - enemy.position
        val dist = toPlayer.length()

        when (enemy.aiState) {
            AIState.APPROACH -> {
                // Dive in at 1.5x speed
                if (dist > 0) {
                    val target = toPlayer.normalized() * enemy.getEffectiveSpeed() * 1.5f
                    enemy.velocity.lerp(target + dodge, 5f * deltaTime)
                }
                // Switch to retreat when close (nova range)
                if (dist < 60f) {
                    enemy.aiState = AIState.RETREAT
                    enemy.stateTimer = 0f
                }
            }
            AIState.RETREAT -> {
                // Retreat until safe distance or timeout
                enemy.stateTimer += deltaTime
                if (dist > 0) {
                    val awayDir = (enemy.position - ship.position).normalized()
                    val target = awayDir * enemy.getEffectiveSpeed()
                    enemy.velocity.lerp(target + dodge, 5f * deltaTime)
                }
                if (dist > 300f || enemy.stateTimer > 3f) {
                    enemy.aiState = AIState.APPROACH
                    enemy.stateTimer = 0f
                }
            }
            else -> {
                enemy.aiState = AIState.APPROACH
                enemy.stateTimer = 0f
            }
        }

        rotateToward(enemy, toPlayer, deltaTime)
    }

    // ── Tempest — Wanderer ────────────────────────────────────────────────────
    // Random direction changes every 2-3s. No preferred distance. Soft tether to
    // player at 500f (drifts back if too far). 0.8x speed. Environmental hazard.

    private fun updateWanderer(enemy: EnemyShip, ship: Ship, dodge: Vector2, deltaTime: Float) {
        val toPlayer = ship.position - enemy.position
        val dist = toPlayer.length()

        enemy.stateTimer += deltaTime

        // Random direction change every 2-3s
        if (enemy.stateTimer > 2f + Random.nextFloat()) {
            // Pick a new random direction
            val randomAngle = Random.nextFloat() * 2f * PI.toFloat()
            val wanderTarget = Vector2(cos(randomAngle), sin(randomAngle)) * enemy.getEffectiveSpeed() * 0.8f

            // Soft tether: if too far from player, bias direction toward player
            if (dist > 500f && dist > 0) {
                val tetherBias = toPlayer.normalized() * 0.5f
                val combined = (Vector2(cos(randomAngle), sin(randomAngle)) + tetherBias).normalized()
                enemy.velocity.lerp(combined * enemy.getEffectiveSpeed() * 0.8f + dodge, 5f * deltaTime)
            } else {
                enemy.velocity.lerp(wanderTarget + dodge, 5f * deltaTime)
            }

            enemy.stateTimer = 0f
        } else {
            // Continue current direction with soft tether
            if (dist > 500f && dist > 0) {
                val tetherBias = toPlayer.normalized() * 0.3f
                val current = if (enemy.velocity.length() > 0) enemy.velocity.normalized() else Vector2(1f, 0f)
                val combined = (current + tetherBias).normalized() * enemy.getEffectiveSpeed() * 0.8f
                enemy.velocity.lerp(combined + dodge, 3f * deltaTime)
            } else {
                applyDodge(enemy, dodge, deltaTime)
            }
        }

        rotateToward(enemy, toPlayer, deltaTime)
    }

    // ── Ripper — Aggressive Brawler ───────────────────────────────────────────
    // Fastest enemy (1.5x speed). Charges straight at player. Within 100f, tight
    // circling with inward bias. Within 50f, presses toward player to keep saw disc on target.

    private fun updateAggressiveBrawler(enemy: EnemyShip, ship: Ship, dodge: Vector2, deltaTime: Float) {
        val toPlayer = ship.position - enemy.position
        val dist = toPlayer.length()
        val fastSpeed = enemy.getEffectiveSpeed() * 1.5f

        if (dist > 100f) {
            // Charge straight at player
            if (dist > 0) {
                val target = toPlayer.normalized() * fastSpeed
                enemy.velocity.lerp(target + dodge, 5f * deltaTime)
            }
        } else {
            // Within 100f: circling with inward bias
            if (dist > 0) {
                val toPlayerNorm = toPlayer.normalized()
                val perp = toPlayerNorm.perpendicular() * enemy.circleDirection
                // Within 50f: press toward player so saw disc makes contact
                val combined = if (dist < 50f) {
                    (toPlayerNorm * 0.8f + perp * 0.2f).normalized() * fastSpeed
                } else {
                    (toPlayerNorm * 0.3f + perp * 0.7f).normalized() * fastSpeed
                }
                enemy.velocity.lerp(combined + dodge, 5f * deltaTime)
            }

            // Occasionally switch circle direction
            enemy.stateTimer += deltaTime
            if (enemy.stateTimer > 2f && Random.nextFloat() < 0.03f) {
                enemy.circleDirection *= -1f
                enemy.stateTimer = 0f
            }
        }

        rotateToward(enemy, toPlayer, deltaTime)
    }

    // ── Specter — Sniper ──────────────────────────────────────────────────────
    // CLOAK (4s repositioning at 0.7x speed, cloaked) → ATTACK (stop, decloak
    // over 0.5s, set target line, isChargingShot=true, 2s total, then recloak
    // and switch strafe direction).

    private fun updateSniper(enemy: EnemyShip, ship: Ship, dodge: Vector2, deltaTime: Float) {
        val toPlayer = ship.position - enemy.position

        when (enemy.aiState) {
            AIState.CLOAK -> {
                // Cloaked — barely visible, repositioning
                enemy.isCloaked = true
                enemy.cloakAlpha = 0.15f
                enemy.isChargingShot = false

                enemy.chargeUpTimer += deltaTime

                // Pick a reposition target on first entering cloak
                if (enemy.chargeUpTimer < deltaTime * 2f) {
                    val angle = Random.nextFloat() * 2f * PI.toFloat()
                    enemy.repositionTargetX = ship.position.x + cos(angle) * enemy.preferredDistance * 1.2f
                    enemy.repositionTargetY = ship.position.y + sin(angle) * enemy.preferredDistance * 1.2f
                }

                // Move toward reposition target at 0.7x speed
                val toTarget = Vector2(
                    enemy.repositionTargetX - enemy.position.x,
                    enemy.repositionTargetY - enemy.position.y
                )
                if (toTarget.length() > 20f) {
                    val target = toTarget.normalized() * enemy.getEffectiveSpeed() * 0.7f
                    enemy.velocity.lerp(target + dodge, 3f * deltaTime)
                } else {
                    enemy.velocity.lerp(dodge, 5f * deltaTime)
                }

                // After 4s, start decloaking
                if (enemy.chargeUpTimer > 4f) {
                    enemy.aiState = AIState.ATTACK
                    enemy.chargeUpTimer = 0f
                    enemy.isChargingShot = true
                    enemy.targetLineX = ship.position.x
                    enemy.targetLineY = ship.position.y
                }

                rotateToward(enemy, toPlayer, deltaTime)
            }
            AIState.ATTACK -> {
                // Decloaking and aiming phase — stop moving
                enemy.chargeUpTimer += deltaTime
                enemy.isChargingShot = true

                // Stop moving
                enemy.velocity.lerp(Vector2(), 8f * deltaTime)

                // Track player position for targeting line
                enemy.targetLineX = ship.position.x
                enemy.targetLineY = ship.position.y

                // Decloak over 0.5 seconds
                if (enemy.chargeUpTimer < 0.5f) {
                    enemy.cloakAlpha = 0.15f + (enemy.chargeUpTimer / 0.5f) * 0.85f
                    enemy.isCloaked = true
                } else {
                    enemy.cloakAlpha = 1f
                    enemy.isCloaked = false
                }

                // After 2s total, recloak and switch strafe direction
                if (enemy.chargeUpTimer > 2f) {
                    enemy.aiState = AIState.CLOAK
                    enemy.chargeUpTimer = 0f
                    enemy.isChargingShot = false
                    enemy.isCloaked = true
                    enemy.cloakAlpha = 0.15f
                    enemy.circleDirection *= -1f  // Switch strafe direction
                }

                rotateToward(enemy, toPlayer, deltaTime)
            }
            else -> {
                // Default to cloak
                enemy.aiState = AIState.CLOAK
                enemy.chargeUpTimer = 0f
                enemy.isCloaked = true
                enemy.cloakAlpha = 0.15f
            }
        }
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    /**
     * Smooth rotation toward a target direction.
     */
    private fun rotateToward(enemy: EnemyShip, toTarget: Vector2, deltaTime: Float) {
        if (toTarget.length() <= 0f) return
        val targetRotation = toTarget.angle()
        val maxTurnRate = 4f
        var diff = targetRotation - enemy.rotation
        while (diff < -PI) diff += 2 * PI.toFloat()
        while (diff > PI) diff -= 2 * PI.toFloat()
        val maxTurn = maxTurnRate * deltaTime
        enemy.rotation += diff.coerceIn(-maxTurn, maxTurn)
    }

    /**
     * Apply dodge vectors to enemy velocity, capping at 1.5x effective speed.
     */
    private fun applyDodge(enemy: EnemyShip, dodge: Vector2, deltaTime: Float) {
        if (dodge.length() > 1f) {
            val dodged = enemy.velocity + dodge
            if (dodged.length() > enemy.getEffectiveSpeed() * 1.5f) {
                val capped = dodged.normalized() * enemy.getEffectiveSpeed() * 1.5f
                enemy.velocity.set(capped)
            } else {
                enemy.velocity.set(dodged)
            }
        }
    }

    // ── Universal dodge and intercept systems ─────────────────────────────────

    private fun calculateMeteorDodge(enemy: EnemyShip, asteroids: List<Asteroid>): Vector2 {
        val dodgeVector = Vector2()

        for (asteroid in asteroids) {
            if (!asteroid.isActive) continue

            val dx = enemy.position.x - asteroid.position.x
            val dy = enemy.position.y - asteroid.position.y
            val dist = sqrt(dx * dx + dy * dy)
            val dangerDist = METEOR_DODGE_RANGE + asteroid.radius + enemy.radius

            if (dist < dangerDist && dist > 1f) {
                // Calculate dodge force - stronger when closer
                val urgency = 1f - (dist / dangerDist)
                val force = DODGE_STRENGTH * urgency * urgency
                dodgeVector.x += (dx / dist) * force
                dodgeVector.y += (dy / dist) * force
            }
        }

        return dodgeVector
    }

    private fun calculateTrailDodge(enemy: EnemyShip, asteroids: List<Asteroid>): Vector2 {
        val dodgeVector = Vector2()
        val dodgeRange = 120f

        for (asteroid in asteroids) {
            if (!asteroid.isActive || asteroid.type != AsteroidType.TRAIL) continue
            // Sample every 4th point to reduce cost
            for (i in asteroid.trailPoints.indices step 4) {
                val point = asteroid.trailPoints[i]
                val dx = enemy.position.x - point.x
                val dy = enemy.position.y - point.y
                val distSq = dx * dx + dy * dy
                if (distSq < dodgeRange * dodgeRange && distSq > 1f) {
                    val dist = sqrt(distSq)
                    val urgency = 1f - (dist / dodgeRange)
                    val force = DODGE_STRENGTH * urgency * urgency
                    dodgeVector.x += (dx / dist) * force
                    dodgeVector.y += (dy / dist) * force
                }
            }
        }
        return dodgeVector
    }

    private fun calculateProjectileDodge(enemy: EnemyShip, projectiles: List<Projectile>): Vector2 {
        val dodgeVector = Vector2()

        for (projectile in projectiles) {
            if (!projectile.isActive) continue

            val dx = enemy.position.x - projectile.position.x
            val dy = enemy.position.y - projectile.position.y
            val dist = sqrt(dx * dx + dy * dy)

            // Mines get special treatment - larger dodge range
            if (projectile.type == ProjectileType.MINE) {
                val dangerDist = MINE_DODGE_RANGE + projectile.explosionRadius + enemy.radius

                if (dist < dangerDist && dist > 1f) {
                    // Strong dodge from mines
                    val urgency = 1f - (dist / dangerDist)
                    val force = DODGE_STRENGTH * 1.5f * urgency * urgency
                    dodgeVector.x += (dx / dist) * force
                    dodgeVector.y += (dy / dist) * force
                }
            } else {
                // Regular projectiles - check if heading toward enemy
                val dangerDist = PROJECTILE_DODGE_RANGE + enemy.radius

                if (dist < dangerDist && dist > 1f) {
                    // Check if projectile is heading toward enemy
                    val projVelLength = sqrt(projectile.velocity.x * projectile.velocity.x +
                            projectile.velocity.y * projectile.velocity.y)

                    if (projVelLength > 0) {
                        // Dot product to see if projectile is heading our way
                        val projDirX = projectile.velocity.x / projVelLength
                        val projDirY = projectile.velocity.y / projVelLength
                        val toEnemyX = dx / dist
                        val toEnemyY = dy / dist
                        val dot = -(projDirX * toEnemyX + projDirY * toEnemyY)

                        if (dot > 0.3f) {  // Projectile is heading toward enemy
                            val urgency = 1f - (dist / dangerDist)
                            val force = DODGE_STRENGTH * urgency * dot

                            // Dodge perpendicular to projectile direction
                            val perpX = -projDirY
                            val perpY = projDirX

                            // Choose direction that moves us away from projectile path
                            val side = if (toEnemyX * perpX + toEnemyY * perpY > 0) 1f else -1f
                            dodgeVector.x += perpX * side * force
                            dodgeVector.y += perpY * side * force
                        }
                    }
                }
            }
        }

        return dodgeVector
    }

    private fun tryInterceptMissile(enemy: EnemyShip, projectiles: List<Projectile>) {
        // Only attempt interception if enemy can fire (has available cooldown)
        if (!enemy.canFire()) return

        // Find homing missiles heading toward this enemy
        for (projectile in projectiles) {
            if (!projectile.isActive) continue
            if (projectile.homingStrength <= 0f) continue  // Not a homing missile

            val dx = projectile.position.x - enemy.position.x
            val dy = projectile.position.y - enemy.position.y
            val dist = sqrt(dx * dx + dy * dy)

            if (dist < MISSILE_INTERCEPT_RANGE && dist > 10f) {
                // Check if this missile is targeting us
                val target = projectile.target
                if (target == enemy || (target == null && projectile.homingStrength > 0)) {
                    // 90% chance to intercept
                    if (Random.nextFloat() < MISSILE_INTERCEPT_CHANCE) {
                        // Fire intercepting laser
                        val dirX = dx / dist
                        val dirY = dy / dist

                        val interceptor = projectilePool.obtain()
                        interceptor.initialize(
                            x = enemy.position.x + dirX * enemy.radius,
                            y = enemy.position.y + dirY * enemy.radius,
                            vx = dirX * 800f,  // Fast interceptor
                            vy = dirY * 800f,
                            projectileType = ProjectileType.BULLET,
                            projectileDamage = 50f,  // Enough to destroy missile
                            projectileLifetime = 0.3f  // Short range
                        )
                        interceptor.weaponId = "enemy_intercept"
                        interceptor.isEnemyProjectile = true
                        interceptor.color = 0xFFFF2233.toInt()  // Bright red enemy

                        enemy.resetFireCooldown()
                        return  // Only one intercept attempt per update
                    }
                }
            }
        }
    }
}

package com.astroloop.game.system

import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.GameState
import com.astroloop.game.core.SoundManager
import com.astroloop.game.entity.*
import kotlin.math.cos
import kotlin.math.sin

class ProjectileEffectsSystem(
    private val ship: Ship,
    private val state: GameState,
    private val collisionSystem: CollisionSystem,
    private val visualEffects: VisualEffectManager,
    private val onAsteroidDestroyed: (Asteroid) -> Unit,
    private val onEnemyDestroyed: (EnemyShip) -> Unit,
    private val onPlayerDeath: () -> Unit,
) {

    private fun isOnScreen(entity: Entity): Boolean {
        val margin = 50f
        val cameraX = ship.position.x - state.screenWidth / 2f
        val cameraY = ship.position.y - state.screenHeight / 2f
        return entity.position.x >= cameraX - margin &&
               entity.position.x <= cameraX + state.screenWidth + margin &&
               entity.position.y >= cameraY - margin &&
               entity.position.y <= cameraY + state.screenHeight + margin
    }

    fun processExpired(projectiles: List<Projectile>, asteroids: List<Asteroid>, enemies: List<EnemyShip>) {
        for (projectile in projectiles) {
            if (projectile.expiredNaturally && !projectile.isActive) {
                processEndOfLifeEffect(projectile)
                processLightningDamageNumbers(projectile)
                processOrbDetonation(projectile)
                processExplosionOnDespawn(projectile, asteroids, enemies)
                projectile.expiredNaturally = false  // Clear flag
            }
        }
    }

    private fun processEndOfLifeEffect(projectile: Projectile) {
        // Enemy projectile end-of-life effects — red, type-scaled. Bullets flash in their
        // own color: normal enemy bullets are red anyway, and crystal-reckoning bullets
        // are cyan — a hardcoded red pop on those read as the wrong entity dying.
        if (projectile.isEnemyProjectile) {
            when (projectile.type) {
                ProjectileType.BULLET -> visualEffects.addHitFlash(
                    projectile.position.x, projectile.position.y, 14f, projectile.color
                )
                ProjectileType.MISSILE -> visualEffects.addExplosion(
                    projectile.position.x, projectile.position.y, 22f, Boss.CORRUPTION_COLOR
                )
                ProjectileType.PLASMA -> visualEffects.addHitFlash(
                    projectile.position.x, projectile.position.y, 18f, Boss.CORRUPTION_COLOR
                )
                ProjectileType.LIGHTNING -> visualEffects.addHitFlash(
                    projectile.position.x, projectile.position.y, 32f, Boss.CORRUPTION_COLOR
                )
                else -> {} // FLAK/TORPEDO/MINE: explodeOnDeath handles it; ORBITER: no death effect
            }
            return
        }

        // Weapon-appropriate end-of-life effects
        when (projectile.weaponId) {
            // Missiles: small explosion
            "homing_missiles" -> visualEffects.addExplosion(
                projectile.position.x, projectile.position.y, 25f, 0xFFFF8833.toInt()
            )
            "autonomous_ace" -> visualEffects.addExplosion(
                projectile.position.x, projectile.position.y, 25f, 0xFF44FF44.toInt()
            )
            // Bolts: small energy burst
            "storm_cannon" -> visualEffects.addExplosion(
                projectile.position.x, projectile.position.y, 20f, 0xFF88BBFF.toInt()
            )
            // Bullets: spark
            "pulse_cannon" -> visualEffects.addHitFlash(
                projectile.position.x, projectile.position.y, 16f, 0xFFFFFF44.toInt()
            )
            "scatter_shot", "leech_burst" -> visualEffects.addHitFlash(
                projectile.position.x, projectile.position.y, 12f, projectile.color
            )
            // Needles: tiny pop
            "needle_gun", "siphon_needles" -> visualEffects.addHitFlash(
                projectile.position.x, projectile.position.y, 10f, projectile.color
            )
            // Beams: energy dissipation
            "railgun" -> visualEffects.addHitFlash(
                projectile.position.x, projectile.position.y, 20f, projectile.color
            )
            else -> {
                // Default small flash for unlisted weapons (including enemy projectiles)
                if (projectile.type !in listOf(ProjectileType.GRAVITY, ProjectileType.ORBITER, ProjectileType.PLASMA)) {
                    visualEffects.addHitFlash(
                        projectile.position.x, projectile.position.y, 8f, projectile.color
                    )
                }
            }
        }
    }

    private fun processLightningDamageNumbers(projectile: Projectile) {
        // Show damage numbers for instant-hit lightning effects (Solar Storm, Phoenix Flare)
        if (projectile.type == ProjectileType.LIGHTNING && projectile.bounceCount == 99 && projectile.damage > 0) {
            visualEffects.addDamageNumber(
                projectile.position.x + (kotlin.random.Random.nextFloat() - 0.5f) * 20f,
                projectile.position.y - 20f,
                projectile.damage.toInt(),
                projectile.color,
                projectile.isCrit
            )
        }
    }

    private fun processOrbDetonation(projectile: Projectile) {
        // Bomber (Quasar) orb detonation: spawn ring of 8 bullets on expiry
        if (projectile.weaponId == "enemy_orb" && projectile.isEnemyProjectile) {
            visualEffects.addExplosion(
                projectile.position.x,
                projectile.position.y,
                40f,
                0xFFFF3344.toInt()
            )
            for (i in 0 until 8) {
                val angle = (2 * Math.PI * i / 8).toFloat()
                val ringProjectile = EntityPools.projectiles.obtain()
                ringProjectile.initialize(
                    x = projectile.position.x,
                    y = projectile.position.y,
                    vx = cos(angle) * 250f,
                    vy = sin(angle) * 250f,
                    projectileType = ProjectileType.BULLET,
                    projectileDamage = projectile.damage * 0.5f,
                    projectileLifetime = 1.5f
                )
                ringProjectile.weaponId = "enemy_orb_ring"
                ringProjectile.isEnemyProjectile = true
                ringProjectile.color = 0xFFFF3344.toInt()
            }
        }
    }

    private fun processExplosionOnDespawn(projectile: Projectile, asteroids: List<Asteroid>, enemies: List<EnemyShip>) {
        // Bomblet spawning (any TORPEDO with bombletCount > 0)
        if (projectile.explodeOnDeath && projectile.bombletCount > 0 && projectile.type == ProjectileType.TORPEDO) {
            spawnBomblets(projectile)
        }

        // Cluster bomb bomblet detonation → spawn fragments (L1+, fragments are core identity)
        if (projectile.type == ProjectileType.BOMBLET && projectile.hasFragments && projectile.explodeOnDeath) {
            spawnFragments(projectile)
        }


        // Flak, torpedo, missile, mine, bomblet, fragment, and volatile detonation explode on despawn
        if (projectile.explodeOnDeath && (projectile.type == ProjectileType.FLAK || projectile.type == ProjectileType.TORPEDO || projectile.type == ProjectileType.MISSILE || projectile.type == ProjectileType.MINE || projectile.type == ProjectileType.BOMBLET || projectile.type == ProjectileType.FRAGMENT || projectile.weaponId == "volatile_detonation")) {
            if (projectile.type == ProjectileType.FLAK) {
                visualEffects.addFlakExplosion(
                    projectile.position.x,
                    projectile.position.y,
                    projectile.explosionRadius,
                    listOf(35, 36, 38).random()
                )
            } else {
                val explosionColor = if (projectile.isEnemyProjectile) 0xFFFF4444.toInt() else 0xFFFFAA44.toInt()
                visualEffects.addExplosion(
                    projectile.position.x,
                    projectile.position.y,
                    projectile.explosionRadius,
                    explosionColor
                )
            }
            SoundManager.playSFX("sfx_explosion", volume = 0.4f)

            // Explosions always damage asteroids
            val nearbyAsteroids = collisionSystem.getAsteroidsInRadius(
                projectile.position.x,
                projectile.position.y,
                projectile.explosionRadius,
                asteroids
            )
            for (asteroid in nearbyAsteroids) {
                if (!projectile.isEnemyProjectile) {
                    val isCrit = state.rollCrit()
                    val dmg = if (isCrit) projectile.explosionDamage * GameConfig.CRIT_DAMAGE_MULTIPLIER else projectile.explosionDamage
                    state.telemetryDamageByWeapon[projectile.weaponId] = (state.telemetryDamageByWeapon[projectile.weaponId] ?: 0f) + dmg
                    state.telemetryTotalDamageDealt += dmg
                    if (isCrit) { state.telemetryCritsThisMinute++; state.telemetryCritsTotal++ }
                    // Damage number colored by the weapon (matches its particles /
                    // direct-hit numbers) — the orange burst above is the explosion
                    // visual. Without this, airburst weapons (flak especially) never
                    // show numbers against asteroids because the proximity fuse skips
                    // the direct-hit path.
                    visualEffects.addDamageNumber(
                        asteroid.position.x,
                        asteroid.position.y - asteroid.radius,
                        dmg.toInt(),
                        projectile.color,
                        isCrit
                    )
                    if (asteroid.takeDamage(dmg)) {
                        onAsteroidDestroyed(asteroid)
                    }
                } else {
                    if (asteroid.takeDamage(projectile.explosionDamage)) {
                        onAsteroidDestroyed(asteroid)
                    }
                }
            }

            // Player projectile explosions damage enemies
            if (!projectile.isEnemyProjectile) {
                val nearbyEnemies = collisionSystem.getEnemiesInRadius(
                    projectile.position.x,
                    projectile.position.y,
                    projectile.explosionRadius,
                    enemies
                )
                for (enemy in nearbyEnemies) {
                    if (!enemy.isCrewmate && !isOnScreen(enemy)) continue  // Can't damage off-screen enemies
                    val isCrit = state.rollCrit()
                    val explosionDmg = if (isCrit) projectile.explosionDamage * GameConfig.CRIT_DAMAGE_MULTIPLIER else projectile.explosionDamage
                    state.telemetryDamageByWeapon[projectile.weaponId] = (state.telemetryDamageByWeapon[projectile.weaponId] ?: 0f) + explosionDmg
                    state.telemetryTotalDamageDealt += explosionDmg
                    if (isCrit) { state.telemetryCritsThisMinute++; state.telemetryCritsTotal++ }
                    enemy.takeDamage(explosionDmg)
                    if (!enemy.perfectDodge && !enemy.isSpawnShielded) {
                        visualEffects.addDamageNumber(
                            enemy.position.x,
                            enemy.position.y - enemy.radius,
                            explosionDmg.toInt(),
                            projectile.color,
                            isCrit
                        )
                    }
                    if (!enemy.isActive) {
                        onEnemyDestroyed(enemy)
                    }
                }
            }

            // Enemy explosions and volatile detonations damage the player
            if (projectile.isEnemyProjectile || projectile.weaponId == "volatile_detonation") {
                val dx = ship.position.x - projectile.position.x
                val dy = ship.position.y - projectile.position.y
                val distSq = dx * dx + dy * dy
                val damageRadius = projectile.explosionRadius + ship.radius
                // Crystal powers grant volatile immunity
                val volatileImmune = projectile.weaponId == "volatile_detonation" && state.hasCrystalPowers
                if (distSq <= damageRadius * damageRadius && !ship.isInvulnerable && !volatileImmune) {
                    ship.takeDamage(projectile.explosionDamage)
                    visualEffects.addDamageNumber(
                        ship.position.x,
                        ship.position.y - ship.radius,
                        projectile.explosionDamage.toInt(),
                        0xFFFF4444.toInt()  // Red for player damage
                    )
                    if (ship.health <= 0) {
                        onPlayerDeath()
                    }
                }
            }
        }
    }

    fun spawnBombletsFromCollision(projectile: Projectile) {
        // Spawn bomblets when cluster bomb torpedo is killed by collision (hit asteroid/enemy)
        spawnBomblets(projectile)
        // Add explosion visual at detonation point
        visualEffects.addExplosion(
            projectile.position.x,
            projectile.position.y,
            projectile.explosionRadius,
            0xFFFFAA44.toInt()
        )
        SoundManager.playSFX("sfx_explosion", volume = 0.4f)
    }

    private fun spawnBomblets(projectile: Projectile) {
        val count = projectile.bombletCount
        for (i in 0 until count) {
            val angle = (2 * Math.PI * i / count).toFloat()
            val bomblet = EntityPools.projectiles.obtain()
            bomblet.initialize(
                x = projectile.position.x,
                y = projectile.position.y,
                vx = cos(angle) * 150f,
                vy = sin(angle) * 150f,
                projectileType = ProjectileType.BOMBLET,
                projectileDamage = projectile.bombletDamage,
                projectileLifetime = 1f
            )
            bomblet.weaponId = "cluster_bomb"
            bomblet.isEnemyProjectile = projectile.isEnemyProjectile
            bomblet.explodeOnDeath = true
            bomblet.explosionRadius = projectile.bombletExplosionRadius
            bomblet.explosionDamage = projectile.bombletDamage
            bomblet.hasFragments = projectile.hasFragments
            bomblet.fragmentDamage = projectile.fragmentDamage
            bomblet.color = projectile.color

        }
    }

    private fun spawnFragments(projectile: Projectile) {
        // 5 fragments per bomblet at 72-degree spread
        val baseAngle = projectile.velocity.angle()
        for (i in 0 until 5) {
            val spreadAngle = baseAngle + (i - 2) * (2 * Math.PI / 5).toFloat()
            val fragment = EntityPools.projectiles.obtain()
            fragment.initialize(
                x = projectile.position.x,
                y = projectile.position.y,
                vx = cos(spreadAngle) * 120f,
                vy = sin(spreadAngle) * 120f,
                projectileType = ProjectileType.FRAGMENT,
                projectileDamage = projectile.fragmentDamage,
                projectileLifetime = 1f
            )
            fragment.weaponId = "cluster_bomb"
            fragment.isEnemyProjectile = projectile.isEnemyProjectile
            fragment.explodeOnDeath = true
            fragment.explosionRadius = projectile.bombletExplosionRadius * 0.67f  // ~40% of main
            fragment.explosionDamage = projectile.fragmentDamage
            fragment.color = projectile.color
        }
    }
}

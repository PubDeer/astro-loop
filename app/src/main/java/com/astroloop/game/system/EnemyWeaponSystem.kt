package com.astroloop.game.system

import com.astroloop.game.entity.*
import com.astroloop.game.core.SoundManager
import kotlin.math.*
import kotlin.random.Random

class EnemyWeaponSystem(
    private val projectilePool: EntityPool<Projectile>
) {
    companion object {
        val CORRUPTION_COLOR = 0xFFAA2222.toInt()
    }

    fun update(enemies: List<EnemyShip>, ship: Ship, deltaTime: Float) {
        for (enemy in enemies) {
            if (!enemy.isActive || enemy.isWarping || enemy.isCrewmate) continue
            if (enemy.weaponCooldown > 0f) {
                enemy.weaponCooldown -= deltaTime
            }
            when (enemy.weaponId) {
                "ion_orbiters" -> updateOrbiters(enemy, deltaTime)
            }
            // Solar Storm telegraph: tick down and fire on expiry
            if (enemy.solarTelegraphActive) {
                enemy.solarTelegraphTimer -= deltaTime
                if (enemy.solarTelegraphTimer <= 0f) {
                    enemy.solarTelegraphActive = false
                    spawnSolarStrikeLightning(enemy, ship)
                }
            }
        }
    }

    fun fire(enemy: EnemyShip, ship: Ship) {
        if (enemy.weaponCooldown > 0f) return

        val dx = ship.position.x - enemy.position.x
        val dy = ship.position.y - enemy.position.y
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val dirX = dx / dist
        val dirY = dy / dist
        val spawnX = enemy.position.x + dirX * enemy.radius
        val spawnY = enemy.position.y + dirY * enemy.radius

        // Weapon-specific firing conditions
        when (enemy.weaponId) {
            "scatter_shot" -> if (dist > 150f) return  // Only fire at close range
            "nova_blast" -> if (dist > 80f) return  // Only fire at point-blank (diver gets close first)
            "space_mines" -> if (enemy.velocity.length() < 50f) return  // Only drop while moving
            "energy_saw" -> return  // Never fires projectiles — damage via disc contact
            "railgun" -> {
                // Specter only fires when decloaked and aiming
                if (enemy.isCloaked || enemy.chargeUpTimer < 1.0f) return
            }
            "ion_orbiters" -> { /* Always active — orbiters just exist */ }
            // All other weapons: fire when in range and cooldown ready (no extra conditions)
        }

        when (enemy.weaponId) {
            "pulse_cannon" -> firePulseCannon(enemy, spawnX, spawnY, dirX, dirY)
            "scatter_shot" -> fireScatterShot(enemy, spawnX, spawnY, dirX, dirY)
            "railgun" -> fireRailgun(enemy, spawnX, spawnY, dirX, dirY)
            "needle_gun" -> fireNeedleGun(enemy, spawnX, spawnY, dirX, dirY)
            "flak_cannon" -> fireFlakCannon(enemy, spawnX, spawnY, dirX, dirY)
            "homing_missiles" -> fireHomingMissiles(enemy, spawnX, spawnY, dirX, dirY, ship)
            "cluster_bomb" -> fireClusterBomb(enemy, spawnX, spawnY, dirX, dirY)
            "space_mines" -> fireSpaceMines(enemy)
            "nova_blast" -> fireNovaBlast(enemy)
            "solar_storm" -> fireSolarStorm(enemy, ship)
            "ion_orbiters" -> fireOrbiters(enemy)
            "energy_saw" -> { /* No-op: damage via disc contact */ }
        }
        // Enemy fire reuses the player's weapon sound at reduced volume (off-beat — enemies
        // fire on their own cooldowns). energy_saw returns earlier (contact damage, no shot).
        if (enemy.weaponId != "energy_saw") {
            SoundManager.playSFX("sfx_weapon_${enemy.weaponId}", 0.4f)
        }
    }

    fun onEnemyRemoved(enemy: EnemyShip) {
        for (orbiter in enemy.orbiterProjectiles) {
            orbiter.isActive = false
        }
        enemy.orbiterProjectiles.clear()
    }

    // === SIMPLE PROJECTILE WEAPONS ===

    private fun firePulseCannon(enemy: EnemyShip, spawnX: Float, spawnY: Float, dirX: Float, dirY: Float) {
        val p = projectilePool.obtain()
        p.initialize(spawnX, spawnY, dirX * 450f, dirY * 450f, ProjectileType.BULLET, enemy.weaponDamage, 3f)
        p.isEnemyProjectile = true
        p.color = CORRUPTION_COLOR
        p.radius = 3f
        enemy.weaponCooldown = 1.0f
    }

    private fun fireScatterShot(enemy: EnemyShip, spawnX: Float, spawnY: Float, dirX: Float, dirY: Float) {
        val baseAngle = atan2(dirY, dirX)
        val spreadCount = 3
        val totalSpread = 0.5f
        for (i in 0 until spreadCount) {
            val angle = baseAngle + (i - (spreadCount - 1) / 2f) * (totalSpread / (spreadCount - 1))
            val p = projectilePool.obtain()
            p.initialize(spawnX, spawnY, cos(angle) * 400f, sin(angle) * 400f, ProjectileType.BULLET, enemy.weaponDamage, 1.5f)
            p.isEnemyProjectile = true
            p.color = CORRUPTION_COLOR
            p.radius = 3f
        }
        enemy.weaponCooldown = 1.2f
    }

    private fun fireRailgun(enemy: EnemyShip, spawnX: Float, spawnY: Float, dirX: Float, dirY: Float) {
        val p = projectilePool.obtain()
        p.initialize(spawnX, spawnY, dirX * 1200f, dirY * 1200f, ProjectileType.BULLET, enemy.weaponDamage * 2.5f, 2f)
        p.isEnemyProjectile = true
        p.color = CORRUPTION_COLOR
        p.radius = 10f          // mirror the player L5 rail width
        p.piercing = true
        p.length = 40f
        p.width = 11f
        enemy.weaponCooldown = 2.5f
    }

    private fun fireNeedleGun(enemy: EnemyShip, spawnX: Float, spawnY: Float, dirX: Float, dirY: Float) {
        val baseAngle = atan2(dirY, dirX)
        val spread = 0.08f
        val angle = baseAngle + (Random.nextFloat() - 0.5f) * spread
        val p = projectilePool.obtain()
        p.initialize(spawnX, spawnY, cos(angle) * 600f, sin(angle) * 600f, ProjectileType.BULLET, enemy.weaponDamage * 0.3f, 2f)
        p.isEnemyProjectile = true
        p.color = CORRUPTION_COLOR
        p.radius = 2f
        enemy.weaponCooldown = 0.15f
    }

    private fun fireFlakCannon(enemy: EnemyShip, spawnX: Float, spawnY: Float, dirX: Float, dirY: Float) {
        val baseAngle = atan2(dirY, dirX)
        for (i in 0..1) {
            val angle = baseAngle + (i - 0.5f) * 0.15f
            val p = projectilePool.obtain()
            p.initialize(spawnX, spawnY, cos(angle) * 350f, sin(angle) * 350f, ProjectileType.FLAK, enemy.weaponDamage, 2f)
            p.isEnemyProjectile = true
            p.color = CORRUPTION_COLOR
            p.radius = 6f  // Match player flak size
            p.explodeOnDeath = true
            p.explosionRadius = 35f
            p.explosionDamage = enemy.weaponDamage * 0.5f
        }
        enemy.weaponCooldown = 1.5f
    }

    private fun fireHomingMissiles(enemy: EnemyShip, spawnX: Float, spawnY: Float, dirX: Float, dirY: Float, ship: Ship) {
        val p = projectilePool.obtain()
        p.initialize(spawnX, spawnY, dirX * 200f, dirY * 200f, ProjectileType.MISSILE, enemy.weaponDamage, 4f)
        p.isEnemyProjectile = true
        p.color = CORRUPTION_COLOR
        p.radius = 4f
        p.homingStrength = 1.5f
        p.target = ship
        enemy.weaponCooldown = 2.0f
    }

    private fun fireClusterBomb(enemy: EnemyShip, spawnX: Float, spawnY: Float, dirX: Float, dirY: Float) {
        val p = projectilePool.obtain()
        p.initialize(spawnX, spawnY, dirX * 200f, dirY * 200f, ProjectileType.TORPEDO, enemy.weaponDamage, 6f)
        p.isEnemyProjectile = true
        p.color = CORRUPTION_COLOR
        p.radius = 12f
        p.explodeOnDeath = true
        p.explosionRadius = 80f
        p.explosionDamage = enemy.weaponDamage * 0.3f
        p.bombletCount = 4
        p.bombletDamage = enemy.weaponDamage * 0.3f
        p.bombletExplosionRadius = 48f
        p.hasFragments = true
        p.fragmentDamage = enemy.weaponDamage * 0.15f

        enemy.weaponCooldown = 2.5f
    }

    // === SPECIAL WEAPONS ===

    private fun fireSpaceMines(enemy: EnemyShip) {
        val moveAngle = atan2(enemy.velocity.y, enemy.velocity.x)
        val behindX = enemy.position.x - cos(moveAngle) * enemy.radius
        val behindY = enemy.position.y - sin(moveAngle) * enemy.radius
        val p = projectilePool.obtain()
        p.initialize(behindX, behindY, 0f, 0f, ProjectileType.MINE, enemy.weaponDamage, 8f)
        p.isEnemyProjectile = true
        p.color = CORRUPTION_COLOR
        p.radius = 12f  // Match player mine size
        p.explodeOnDeath = true
        p.explosionRadius = 50f
        p.explosionDamage = enemy.weaponDamage
        enemy.weaponCooldown = 2.0f
    }

    private fun fireNovaBlast(enemy: EnemyShip) {
        val ringCount = 8
        val blastRadius = 180f
        for (i in 0 until ringCount) {
            val angle = (2 * PI.toFloat() * i / ringCount)
            val p = projectilePool.obtain()
            p.initialize(
                x = enemy.position.x, y = enemy.position.y,
                vx = cos(angle) * blastRadius * 3f, vy = sin(angle) * blastRadius * 3f,
                projectileType = ProjectileType.PLASMA,
                projectileDamage = enemy.weaponDamage,
                projectileLifetime = 0.3f
            )
            p.weaponId = "nova_blast"
            p.radius = blastRadius / 3
            p.piercing = true
            p.maxPierces = 1000
            p.isEnemyProjectile = true
            p.color = CORRUPTION_COLOR
        }
        enemy.weaponCooldown = 4.0f
    }

    private fun fireSolarStorm(enemy: EnemyShip, ship: Ship) {
        val dx = ship.position.x - enemy.position.x
        val dy = ship.position.y - enemy.position.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist > 800f) {
            enemy.weaponCooldown = 0.5f
            return
        }
        // Lock target position and start 0.5s telegraph — strike lands after the warning ring
        enemy.solarTelegraphActive = true
        enemy.solarTelegraphTimer = 0.5f
        enemy.solarTelegraphX = ship.position.x
        enemy.solarTelegraphY = ship.position.y
        enemy.weaponCooldown = 2.4f
    }

    private fun spawnSolarStrikeLightning(enemy: EnemyShip, ship: Ship) {
        val lightning = projectilePool.obtain()
        lightning.initialize(
            x = enemy.solarTelegraphX, y = enemy.solarTelegraphY,
            vx = 0f, vy = 0f,
            projectileType = ProjectileType.LIGHTNING,
            projectileDamage = enemy.weaponDamage,
            projectileLifetime = 0.4f
        )
        lightning.isEnemyProjectile = true
        lightning.color = CORRUPTION_COLOR
        lightning.radius = 60f
        lightning.bounceCount = 99
    }

    // === STATEFUL WEAPONS ===

    private fun fireOrbiters(enemy: EnemyShip) {
        enemy.orbiterProjectiles.removeAll { !it.isActive }
        val targetCount = 2
        val damage = enemy.weaponDamage
        val orbitRadius = 70f

        while (enemy.orbiterProjectiles.size < targetCount) {
            val spawnAngle = if (enemy.orbiterProjectiles.isNotEmpty()) {
                enemy.orbiterProjectiles.last().orbitAngle
            } else {
                enemy.orbiterBaseAngle
            }
            val projectile = projectilePool.obtain()
            projectile.initialize(
                x = enemy.position.x, y = enemy.position.y,
                vx = 0f, vy = 0f,
                projectileType = ProjectileType.ORBITER,
                projectileDamage = damage,
                projectileLifetime = 999f
            )
            projectile.weaponId = "ion_orbiters"
            projectile.orbitRadius = orbitRadius
            projectile.orbitSpeed = 0f
            projectile.orbitAngle = spawnAngle
            projectile.radius = 10f
            projectile.piercing = true
            projectile.maxPierces = 1000
            projectile.isEnemyProjectile = true
            projectile.color = CORRUPTION_COLOR
            projectile.fadeAlpha = 0f
            enemy.orbiterProjectiles.add(projectile)
        }

        val total = enemy.orbiterProjectiles.size
        for ((index, orbiter) in enemy.orbiterProjectiles.withIndex()) {
            val targetAngle = enemy.orbiterBaseAngle + (2f * PI.toFloat() * index / total)
            orbiter.orbitAngle = lerpAngle(orbiter.orbitAngle, targetAngle, 0.15f)
            orbiter.orbitRadius = orbitRadius
            orbiter.damage = damage
        }
        enemy.weaponCooldown = 3f
    }

    private fun updateOrbiters(enemy: EnemyShip, deltaTime: Float) {
        enemy.orbiterProjectiles.removeAll { !it.isActive }
        enemy.orbiterBaseAngle += enemy.orbiterOrbitSpeed * deltaTime

        val total = enemy.orbiterProjectiles.size
        for ((index, orbiter) in enemy.orbiterProjectiles.withIndex()) {
            val targetAngle = enemy.orbiterBaseAngle + (2f * PI.toFloat() * index / total)
            orbiter.orbitAngle = lerpAngle(orbiter.orbitAngle, targetAngle, 0.15f)
            orbiter.position.x = enemy.position.x + cos(orbiter.orbitAngle) * orbiter.orbitRadius
            orbiter.position.y = enemy.position.y + sin(orbiter.orbitAngle) * orbiter.orbitRadius
        }
    }

    fun getSawDiscPositions(enemy: EnemyShip): List<Pair<Float, Float>> {
        val positions = mutableListOf<Pair<Float, Float>>()
        positions.add(Pair(
            enemy.position.x + cos(enemy.rotation) * enemy.sawReach,
            enemy.position.y + sin(enemy.rotation) * enemy.sawReach
        ))
        return positions
    }

    private fun lerpAngle(from: Float, to: Float, t: Float): Float {
        var diff = to - from
        while (diff > PI.toFloat()) diff -= 2f * PI.toFloat()
        while (diff < -PI.toFloat()) diff += 2f * PI.toFloat()
        return from + diff * t
    }
}

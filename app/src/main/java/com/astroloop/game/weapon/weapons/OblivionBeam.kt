package com.astroloop.game.weapon.weapons

import com.astroloop.game.core.GameState
import com.astroloop.game.entity.Entity
import com.astroloop.game.entity.EntityPool
import com.astroloop.game.entity.Projectile
import com.astroloop.game.entity.Firer
import com.astroloop.game.weapon.Weapon

class OblivionBeam : Weapon(
    id = "oblivion_beam",
    name = "Oblivion Beam",
    description = "Always-on piercing lance"
) {
    override val baseDamage = 18f     // per tick (10 ticks/sec); was 50 — heavy nerf
    override val baseCooldown = 0.1f   // per-entity tick interval
    override val baseProjectileSpeed = 0f
    override val baseProjectileCount = 1

    override fun getDamage(state: GameState): Float = baseDamage * state.damageMultiplier

    override fun fire(
        firer: Firer,
        state: GameState,
        projectilePool: EntityPool<Projectile>,
        targets: List<Entity>
    ) {
        // No-op: BeamDamageSystem applies continuous damage
    }
}

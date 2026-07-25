package com.astroloop.game.entity

import com.astroloop.game.util.Vector2

interface Firer {
    val position: Vector2
    val velocity: Vector2
    val rotation: Float
    val radius: Float
    /** True when this firer is an enemy — weapons use this to mark spawned projectiles. */
    val isEnemyFirer: Boolean
}

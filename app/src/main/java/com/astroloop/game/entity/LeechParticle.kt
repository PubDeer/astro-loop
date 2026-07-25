package com.astroloop.game.entity

data class LeechParticle(
    val edgeX: Float,   // asteroid edge at spawn time (world space)
    val edgeY: Float,
    var t: Float        // 0.0 = at asteroid edge, 1.0 = at ship
)

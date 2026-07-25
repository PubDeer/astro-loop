package com.astroloop.game.entity

data class SawSpark(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var age: Float = 0f,
    val lifetime: Float = 0.2f,
    val color: Int = 0xFFFFDD44.toInt()
)

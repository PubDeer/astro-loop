package com.astroloop.game.entity

import org.junit.Assert.*
import org.junit.Test

/**
 * Shield invariants for the Ship entity.
 *
 * Regression guard: phoenix_core revival once refilled shields from `shieldCap`
 * (default Float.MAX_VALUE) instead of `maxShield`, leaving currentShield at
 * ~3.4e38 — which renders as Int.MAX_VALUE (2147483647) on the HUD and makes
 * the player unkillable.
 */
class ShipShieldTest {

    @Test
    fun `restoreShields refills to maxShield, never to the uncapped shieldCap`() {
        val ship = Ship()
        ship.maxShield = 120f
        ship.currentShield = 0f
        // shieldCap defaults to Float.MAX_VALUE (only Glass Cannon lowers it)
        assertEquals(Float.MAX_VALUE, ship.shieldCap, 0f)

        ship.restoreShields()

        assertEquals(120f, ship.currentShield, 0.001f)
        // The original bug: currentShield.toInt() == 2147483647
        assertEquals(120, ship.currentShield.toInt())
        assertNotEquals(Int.MAX_VALUE, ship.currentShield.toInt())
    }

    @Test
    fun `restoreShields respects a lowered shieldCap (Glass Cannon)`() {
        val ship = Ship()
        ship.maxShield = 100f
        ship.shieldCap = 0f // Glass Cannon
        ship.currentShield = 0f

        ship.restoreShields()

        assertEquals(0f, ship.currentShield, 0.001f)
    }
}

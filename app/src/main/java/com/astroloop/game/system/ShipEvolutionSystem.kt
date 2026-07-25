package com.astroloop.game.system

import com.astroloop.game.core.GameState
import com.astroloop.game.entity.Ship

class ShipEvolutionSystem {

    fun updateShipVisuals(ship: Ship, state: GameState) {
        // Update evolution stage based on total upgrades
        ship.updateEvolutionStage(state.totalUpgradesCollected)

        // Update max health based on permanent upgrades + passives
        val newMaxHealth = (com.astroloop.game.core.GameConfig.SHIP_BASE_HEALTH + state.getPermanentHealthBonus()) *
                           state.maxHealthMultiplier

        // If max health increased, heal the difference
        if (newMaxHealth > ship.maxHealth) {
            val healthIncrease = newMaxHealth - ship.maxHealth
            ship.health += healthIncrease
        }
        ship.maxHealth = newMaxHealth

        // Update weapon-specific visuals
        ship.updateWeaponVisuals(state.weaponLevels.keys)
    }

    fun getEvolutionStageDescription(stage: Int): String {
        return when (stage) {
            0 -> "Basic Fighter"
            1 -> "Scout"
            2 -> "Interceptor"
            3 -> "Destroyer"
            4 -> "Cruiser"
            5 -> "Battlecruiser"
            6 -> "Dreadnought"
            else -> "Unknown"
        }
    }
}

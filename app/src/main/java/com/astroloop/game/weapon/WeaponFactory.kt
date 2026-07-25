package com.astroloop.game.weapon

import com.astroloop.game.weapon.weapons.*

object WeaponFactory {

    fun createWeapon(weaponId: String): Weapon? {
        return when (weaponId) {
            // Base weapons
            "pulse_cannon" -> PulseCannon()
            "energy_saw" -> EnergySaw()
            "scatter_shot" -> ScatterShot()
            "homing_missiles" -> HomingMissiles()
            "ion_orbiters" -> IonOrbiters()
            "railgun" -> Railgun()
            "space_mines" -> SpaceMines()
            "solar_storm" -> SolarStorm()
            "nova_blast" -> NovaBlast()
            "needle_gun" -> NeedleGun()
            "cluster_bomb" -> ClusterBomb()
            "flak_cannon" -> FlakCannon()

            // Evolution weapons
            "storm_cannon" -> StormCannon()
            "warp_saw" -> WarpSaw()
            "leech_burst" -> LeechBurst()
            "autonomous_ace" -> AutonomousAce()
            "frost_ring" -> FrostRing()
            "oblivion_beam" -> OblivionBeam()
            "jackpot_mines" -> JackpotMines()
            "phoenix_flare" -> PhoenixFlare()
            "lingering_nova" -> LingeringNova()
            "siphon_needles" -> SiphonNeedles()
            "hunter_killer" -> HunterKiller()
            "flak_barrage" -> FlakBarrage()

            else -> null
        }
    }

    fun getBaseWeaponIds(): List<String> = listOf(
        "pulse_cannon",
        "energy_saw",
        "scatter_shot",
        "homing_missiles",
        "ion_orbiters",
        "railgun",
        "space_mines",
        "solar_storm",
        "nova_blast",
        "needle_gun",
        "cluster_bomb",
        "flak_cannon"
    )

    fun getEvolutionIds(): List<String> = listOf(
        "storm_cannon",
        "warp_saw",
        "leech_burst",
        "autonomous_ace",
        "frost_ring",
        "oblivion_beam",
        "jackpot_mines",
        "phoenix_flare",
        "lingering_nova",
        "siphon_needles",
        "hunter_killer",
        "flak_barrage"
    )

    fun isEvolution(weaponId: String): Boolean = getEvolutionIds().contains(weaponId)
}

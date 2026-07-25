package com.astroloop.game.system

import com.astroloop.game.core.GameConfig
import com.astroloop.game.core.GameState
import com.astroloop.game.data.PassiveDefinitions
import com.astroloop.game.data.WeaponDefinitions
import com.astroloop.game.entity.EntityPool
import com.astroloop.game.entity.PowerUp
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for the arsenal gate:
 * getEligibleLevelUps() retains the hard gate — a weapon at level N is only
 * eligible when the player owns >= N distinct weapons.
 *
 * generateUpgradeOptions() uses a soft probability roll instead:
 * a gated weapon may still appear with reduced probability, and is guaranteed
 * to appear when it is the only possible upgrade.
 *
 * Gate table (hard gate, getEligibleLevelUps only):
 *   L1 → L2: always eligible (ownedWeaponCount >= 1, trivially true)
 *   L2 → L3: need 2+ owned weapons
 *   L3 → L4: need 3+ owned weapons
 *   L4 → L5: need 4+ owned weapons
 */
class UpgradeSystemGateTest {

    private lateinit var upgradeSystem: UpgradeSystem
    private lateinit var powerUpPool: EntityPool<PowerUp>

    @Before
    fun setup() {
        powerUpPool = EntityPool({ PowerUp() }, 10)
        upgradeSystem = UpgradeSystem(powerUpPool)
        upgradeSystem.unlockedWeaponIds = WeaponDefinitions.getBaseWeapons().map { it.id }.toSet()
        upgradeSystem.unlockedPassiveIds = PassiveDefinitions.getAllPassives().map { it.id }.toSet()
    }

    // Helper: build a fresh GameState with the given weapon configuration.
    // weaponEntries is a list of (weaponId, times to call addWeapon).
    private fun stateWith(vararg weaponEntries: Pair<String, Int>): GameState {
        val state = GameState()
        state.reset()
        for ((id, times) in weaponEntries) {
            repeat(times) { state.addWeapon(id) }
        }
        return state
    }

    // ─── L2 → L3 gate ───────────────────────────────────────────────

    @Test
    fun `L2 weapon NOT eligible for L3 when player owns only 1 weapon`() {
        // pulse_cannon at L2, only weapon owned — fails ownedWeaponCount(1) >= currentLevel(2)
        val state = stateWith("pulse_cannon" to 2)

        val eligible = upgradeSystem.getEligibleLevelUps(state)

        assertFalse(
            "pulse_cannon at L2 should NOT be offered L3 with only 1 weapon owned",
            eligible.any { it.id == "pulse_cannon" }
        )
    }

    @Test
    fun `L2 weapon IS eligible for L3 when player owns 2 weapons`() {
        // pulse_cannon at L2, railgun at L1 — ownedWeaponCount(2) >= currentLevel(2)
        val state = stateWith("pulse_cannon" to 2, "railgun" to 1)

        val eligible = upgradeSystem.getEligibleLevelUps(state)

        assertTrue(
            "pulse_cannon at L2 should be offered L3 with 2 weapons owned",
            eligible.any { it.id == "pulse_cannon" }
        )
    }

    // ─── L4 → L5 gate ───────────────────────────────────────────────

    @Test
    fun `L4 weapon NOT eligible for L5 when player owns only 3 weapons`() {
        // pulse_cannon at L4, two others at L1 — ownedWeaponCount(3) < currentLevel(4)
        val state = stateWith("pulse_cannon" to 4, "railgun" to 1, "scatter_shot" to 1)

        val eligible = upgradeSystem.getEligibleLevelUps(state)

        assertFalse(
            "pulse_cannon at L4 should NOT be offered L5 with only 3 weapons owned",
            eligible.any { it.id == "pulse_cannon" }
        )
    }

    @Test
    fun `L4 weapon IS eligible for L5 when player owns 4 weapons`() {
        // pulse_cannon at L4, three others at L1 — ownedWeaponCount(4) >= currentLevel(4)
        val state = stateWith(
            "pulse_cannon" to 4,
            "railgun" to 1,
            "scatter_shot" to 1,
            "homing_missiles" to 1
        )

        val eligible = upgradeSystem.getEligibleLevelUps(state)

        assertTrue(
            "pulse_cannon at L4 should be offered L5 with 4 weapons owned",
            eligible.any { it.id == "pulse_cannon" }
        )
    }

    // ─── L1 → L2 always allowed ──────────────────────────────────────

    @Test
    fun `L1 weapon always eligible for L2 regardless of weapon count`() {
        // Single weapon at L1 — ownedWeaponCount(1) >= currentLevel(1)
        val state = stateWith("pulse_cannon" to 1)

        val eligible = upgradeSystem.getEligibleLevelUps(state)

        assertTrue(
            "pulse_cannon at L1 should always be offered L2",
            eligible.any { it.id == "pulse_cannon" }
        )
    }

    // ─── Soft gate in generateUpgradeOptions ────────────────────────

    @Test
    fun `generateUpgradeOptions can offer gated weapon with reduced probability`() {
        // pulse_cannon L2, only weapon — gate not met, but soft roll may still include it
        val state = stateWith("pulse_cannon" to 2)

        var foundCount = 0
        repeat(200) {
            val options = upgradeSystem.generateUpgradeOptions(state)
            if (options.any { it.id == "pulse_cannon" && it.isWeapon && !it.isEvolution }) {
                foundCount++
            }
        }
        // Soft gate probability for L2 solo is 1/2 = 50%; with selection competition the real rate is lower.
        // "> 0 in 200" has vanishingly small failure probability. "< 180" ensures it doesn't saturate.
        assertTrue("Gated weapon should occasionally appear", foundCount > 0)
        assertTrue("Gated weapon should not dominate", foundCount < 180)
    }

    @Test
    fun `generateUpgradeOptions always offers upgrade when gated weapon is only option`() {
        // pulse_cannon L4, only weapon — when everything else is maxed, must still appear
        val state = stateWith("pulse_cannon" to 4)
        // Mark passive slots full so no passives can appear
        repeat(GameConfig.PASSIVE_MAX_STACKS) { state.addPassive("nano_repair") }
        repeat(GameConfig.PASSIVE_MAX_STACKS) { state.addPassive("shield_booster") }
        // Restrict to only pulse_cannon weapon
        upgradeSystem.unlockedWeaponIds = setOf("pulse_cannon")
        upgradeSystem.unlockedPassiveIds = setOf("nano_repair", "shield_booster")

        repeat(50) {
            val options = upgradeSystem.generateUpgradeOptions(state)
            assertTrue(
                "Must always offer pulse_cannon L5 when it is the only possible upgrade",
                options.isNotEmpty() && options.any { it.id == "pulse_cannon" }
            )
        }
    }

    @Test
    fun `generateUpgradeOptions always shows solo weapon when it is the only weapon type (Scout+Medic scenario)`() {
        // Regression: with only 1 weapon + 1 passive unlocked, the soft-gate fallback
        // used to only fire when options was completely empty. Since nano_repair filled a
        // slot, options was never empty, so pulse_cannon was permanently hidden.
        val state = stateWith("pulse_cannon" to 2)
        upgradeSystem.unlockedWeaponIds = setOf("pulse_cannon")
        upgradeSystem.unlockedPassiveIds = setOf("nano_repair")

        repeat(100) {
            val options = upgradeSystem.generateUpgradeOptions(state)
            assertTrue(
                "Solo weapon pulse_cannon must always appear when it is the only weapon type available",
                options.any { it.id == "pulse_cannon" && it.isWeapon && !it.isEvolution }
            )
        }
    }

    @Test
    fun `generateUpgradeOptions offers weapon level-up once hard gate is met`() {
        val state = stateWith("pulse_cannon" to 2, "railgun" to 1)

        var found = false
        repeat(50) {
            val options = upgradeSystem.generateUpgradeOptions(state)
            if (options.any { it.id == "pulse_cannon" && it.isWeapon && !it.isEvolution }) found = true
        }
        assertTrue("pulse_cannon L3 should appear once gate is met", found)
    }
}

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
 * Tests for UpgradeSystem, focusing on:
 * - Upgrade generation and option selection
 * - Slot limits and capacity checks
 * - Evolution triggers and detection
 * - Fallback options when fully upgraded
 * - Weapon-only options for first pick
 */
class UpgradeSystemTest {

    private lateinit var upgradeSystem: UpgradeSystem
    private lateinit var state: GameState
    private lateinit var powerUpPool: EntityPool<PowerUp>

    @Before
    fun setup() {
        powerUpPool = EntityPool({ PowerUp() }, 10)
        upgradeSystem = UpgradeSystem(powerUpPool)
        state = GameState()
        state.reset()
        upgradeSystem.unlockedWeaponIds = WeaponDefinitions.getBaseWeapons().map { it.id }.toSet()
        upgradeSystem.unlockedPassiveIds = PassiveDefinitions.getAllPassives().map { it.id }.toSet()
    }

    // ─── Basic Upgrade Generation ────────────────────────────────────

    @Test
    fun `generateUpgradeOptions returns correct number of options`() {
        val options = upgradeSystem.generateUpgradeOptions(state)
        assertEquals(GameConfig.UPGRADE_CHOICES, options.size)
    }

    @Test
    fun `generateUpgradeOptions returns unique options`() {
        val options = upgradeSystem.generateUpgradeOptions(state)
        val uniqueIds = options.map { it.id }.toSet()
        assertEquals(options.size, uniqueIds.size)
    }

    @Test
    fun `generateUpgradeOptions includes weapons and passives`() {
        // Run multiple times to account for randomness
        var foundWeapon = false
        var foundPassive = false

        repeat(50) {
            state.reset()
            val options = upgradeSystem.generateUpgradeOptions(state)
            if (options.any { it.isWeapon && !it.isEvolution }) foundWeapon = true
            if (options.any { !it.isWeapon && !it.isEvolution && !it.isFallback }) foundPassive = true
            if (foundWeapon && foundPassive) return@repeat
        }

        assertTrue("Should include weapons", foundWeapon)
        assertTrue("Should include passives", foundPassive)
    }

    // ─── Slot Limit Enforcement ──────────────────────────────────────

    @Test
    fun `generateUpgradeOptions respects weapon slot limit for new weapons`() {
        // Fill all weapon slots
        state.addWeapon("pulse_cannon")
        state.addWeapon("scatter_shot")
        state.addWeapon("railgun")
        state.addWeapon("homing_missiles")

        val options = upgradeSystem.generateUpgradeOptions(state)

        // New weapons should not be offered (only levelups of existing ones)
        val newWeaponOptions = options.filter {
            it.isWeapon && !it.isEvolution &&
            state.getWeaponLevel(it.id) == 0
        }

        assertTrue("Should not offer new weapons when slots full", newWeaponOptions.isEmpty())
    }

    @Test
    fun `generateUpgradeOptions allows leveling existing weapons when slots full`() {
        // Add weapon at level 1
        state.addWeapon("pulse_cannon")
        // Fill remaining slots
        state.addWeapon("scatter_shot")
        state.addWeapon("railgun")
        state.addWeapon("homing_missiles")

        // Run multiple times to find the levelup option
        var foundLevelup = false
        repeat(50) {
            val options = upgradeSystem.generateUpgradeOptions(state)
            if (options.any { it.id == "pulse_cannon" && it.isWeapon }) {
                foundLevelup = true
            }
            if (foundLevelup) return@repeat
        }

        assertTrue("Should allow leveling existing weapons", foundLevelup)
    }

    @Test
    fun `generateUpgradeOptions respects passive slot limit`() {
        // Fill all passive slots
        state.addPassive("nano_repair")
        state.addPassive("magnet_field")
        state.addPassive("lucky_star")
        state.addPassive("tb26")

        val options = upgradeSystem.generateUpgradeOptions(state)

        // New passives should not be offered
        val newPassiveOptions = options.filter {
            !it.isWeapon && !it.isEvolution && !it.isFallback &&
            state.getPassiveStacks(it.id) == 0
        }

        assertTrue("Should not offer new passives when slots full", newPassiveOptions.isEmpty())
    }

    // ─── Evolution Eligibility (via Elite Diamond) ────────────────────

    @Test
    fun `evolutions no longer appear in regular upgrade options`() {
        repeat(5) { state.addWeapon("pulse_cannon") }
        state.addPassive("duplicator_core")
        state.survivalTime = 600f

        val options = upgradeSystem.generateUpgradeOptions(state)

        assertFalse("Regular upgrades should never include evolutions",
            options.any { it.isEvolution })
    }

    @Test
    fun `getEligibleEvolutions returns evolution when L5 weapon + passive`() {
        repeat(5) { state.addWeapon("pulse_cannon") }
        state.addPassive("duplicator_core")

        val eligible = upgradeSystem.getEligibleEvolutions(state)

        val evo = eligible.find { it.id == "storm_cannon" }
        assertNotNull("Should find storm_cannon evolution", evo)
        assertEquals("pulse_cannon", evo?.baseWeaponId)
        assertEquals("duplicator_core", evo?.requiredPassiveId)
    }

    @Test
    fun `getEligibleEvolutions empty when weapon not maxed`() {
        repeat(3) { state.addWeapon("pulse_cannon") }
        state.addPassive("duplicator_core")

        val eligible = upgradeSystem.getEligibleEvolutions(state)

        assertTrue("Should have no eligible evolutions", eligible.isEmpty())
    }

    @Test
    fun `getEligibleEvolutions empty when passive not owned`() {
        repeat(5) { state.addWeapon("pulse_cannon") }

        val eligible = upgradeSystem.getEligibleEvolutions(state)

        assertTrue("Should have no eligible evolutions without passive", eligible.isEmpty())
    }

    @Test
    fun `getEligibleEvolutions excludes already evolved`() {
        repeat(5) { state.addWeapon("pulse_cannon") }
        state.addPassive("duplicator_core")
        state.addEvolution("storm_cannon")

        val eligible = upgradeSystem.getEligibleEvolutions(state)

        assertFalse("Should not include already-evolved weapon",
            eligible.any { it.id == "storm_cannon" })
    }

    @Test
    fun `generateEvolutionOptions caps at 3`() {
        // Max out multiple weapons and add their passives
        repeat(5) { state.addWeapon("pulse_cannon") }
        repeat(5) { state.addWeapon("scatter_shot") }
        repeat(5) { state.addWeapon("homing_missiles") }
        repeat(5) { state.addWeapon("energy_saw") }
        state.addPassive("duplicator_core")
        state.addPassive("vampiric_core")
        state.addPassive("tb26")
        state.addPassive("momentum_drive")

        val options = upgradeSystem.generateEvolutionOptions(state)

        assertTrue("Should cap at 3 evolution options", options.size <= 3)
        assertTrue("Should have at least 1 option", options.isNotEmpty())
        assertTrue("All should be evolutions", options.all { it.isEvolution })
    }

    // ─── Fallback Options ────────────────────────────────────────────

    @Test
    fun `generateUpgradeOptions returns empty when fully upgraded`() {
        // Fill and max everything
        val weapons = WeaponDefinitions.getBaseWeapons().take(4)
        weapons.forEach { weapon ->
            repeat(5) { state.addWeapon(weapon.id) }
        }

        val passives = PassiveDefinitions.getAllPassives().take(4)
        passives.forEach { passive ->
            repeat(5) { state.addPassive(passive.id) }
        }

        val options = upgradeSystem.generateUpgradeOptions(state)

        assertTrue("Should return empty list when fully upgraded", options.isEmpty())
    }

    // ─── Weapon-Only Options ─────────────────────────────────────────

    // ─── Astro Loop Evolution Gating ─────────────────────────────────

    @Test
    fun `getEligibleEvolutions empty before 8 minutes in astro loop mode`() {
        state.astroLoopMode = true
        state.survivalTime = 400f  // ~6.7 min — before gate
        repeat(5) { state.addWeapon("pulse_cannon") }
        state.addPassive("duplicator_core")
        val eligible = upgradeSystem.getEligibleEvolutions(state)
        assertTrue("Evolutions gated before 8min", eligible.isEmpty())
    }

    @Test
    fun `getEligibleEvolutions available at 8 minutes in astro loop mode`() {
        state.astroLoopMode = true
        state.survivalTime = 480f  // exactly 8 min
        repeat(5) { state.addWeapon("pulse_cannon") }
        state.addPassive("duplicator_core")
        val eligible = upgradeSystem.getEligibleEvolutions(state)
        assertFalse("Evolutions appear at 8min", eligible.isEmpty())
    }

    @Test
    fun `getEligibleEvolutions empty when evolution already used in astro loop mode`() {
        state.astroLoopMode = true
        state.survivalTime = 600f
        state.astroLoopEvolutionUsed = true
        repeat(5) { state.addWeapon("pulse_cannon") }
        state.addPassive("duplicator_core")
        val eligible = upgradeSystem.getEligibleEvolutions(state)
        assertTrue("No evolutions after one taken", eligible.isEmpty())
    }

    @Test
    fun `getEligibleEvolutions not gated when astroLoopMode false`() {
        state.astroLoopMode = false
        state.survivalTime = 100f
        repeat(5) { state.addWeapon("pulse_cannon") }
        state.addPassive("duplicator_core")
        val eligible = upgradeSystem.getEligibleEvolutions(state)
        assertFalse("Normal mode has no time gate", eligible.isEmpty())
    }

    @Test
    fun `generateWeaponOnlyOptions returns only weapons`() {
        val options = upgradeSystem.generateWeaponOnlyOptions(state)

        assertTrue("All options should be weapons", options.all { it.isWeapon })
    }

    @Test
    fun `generateWeaponOnlyOptions returns correct number of choices`() {
        val options = upgradeSystem.generateWeaponOnlyOptions(state)
        assertEquals(GameConfig.UPGRADE_CHOICES, options.size)
    }

    // ─── Option Selection ────────────────────────────────────────────

    @Test
    fun `selectOption returns correct option`() {
        val options = upgradeSystem.generateUpgradeOptions(state)
        val expectedOption = options[1]

        val selected = upgradeSystem.selectOption(1)

        assertEquals(expectedOption, selected)
    }

    @Test
    fun `selectOption clears pending options`() {
        upgradeSystem.generateUpgradeOptions(state)
        assertTrue(upgradeSystem.hasPendingOptions())

        upgradeSystem.selectOption(0)

        assertFalse(upgradeSystem.hasPendingOptions())
    }

    @Test
    fun `selectOption returns null for invalid index`() {
        upgradeSystem.generateUpgradeOptions(state)

        assertNull(upgradeSystem.selectOption(-1))
        assertNull(upgradeSystem.selectOption(100))
    }

    @Test
    fun `selectOption returns null when no pending options`() {
        assertNull(upgradeSystem.selectOption(0))
    }

    // ─── Available Upgrades Query ────────────────────────────────────

    @Test
    fun `getAvailableUpgrades returns upgrades that can be added`() {
        val available = upgradeSystem.getAvailableUpgrades(state)

        // Should include base weapons
        assertTrue(available.any { it.id == "pulse_cannon" && it.isWeapon })

        // Should include passives
        assertTrue(available.any { it.id == "nano_repair" && !it.isWeapon })
    }

    @Test
    fun `getAvailableUpgrades excludes maxed weapons`() {
        repeat(5) { state.addWeapon("pulse_cannon") }

        val available = upgradeSystem.getAvailableUpgrades(state)

        assertFalse("Should not include maxed weapon",
            available.any { it.id == "pulse_cannon" })
    }

    @Test
    fun `getAvailableUpgrades excludes maxed passives`() {
        repeat(5) { state.addPassive("nano_repair") }

        val available = upgradeSystem.getAvailableUpgrades(state)

        assertFalse("Should not include maxed passive",
            available.any { it.id == "nano_repair" })
    }

    @Test
    fun `getAvailableUpgrades respects slot limits`() {
        // Fill all weapon slots
        state.addWeapon("pulse_cannon")
        state.addWeapon("scatter_shot")
        state.addWeapon("railgun")
        state.addWeapon("homing_missiles")

        val available = upgradeSystem.getAvailableUpgrades(state)

        // Should not include new weapons (ones we don't own)
        val newWeapons = available.filter {
            it.isWeapon && state.getWeaponLevel(it.id) == 0
        }
        assertTrue("Should not include new weapons when slots full", newWeapons.isEmpty())

        // But should include weapons we already own for leveling
        assertTrue(available.any { it.id == "pulse_cannon" })
    }

    @Test
    fun `getAvailableUpgrades excludes one-time passives when owned`() {
        state.addPassive("phoenix_core")

        val available = upgradeSystem.getAvailableUpgrades(state)

        assertFalse("Should not include phoenix_core when owned",
            available.any { it.id == "phoenix_core" })
    }

    @Test
    fun `getAvailableUpgrades excludes extra_weapon_slot when already active`() {
        state.addPassive("extra_weapon_slot")

        val available = upgradeSystem.getAvailableUpgrades(state)

        assertFalse("Should not include extra_weapon_slot when active",
            available.any { it.id == "extra_weapon_slot" })
    }

    // ─── Pending Options Management ──────────────────────────────────

    @Test
    fun `hasPendingOptions returns false initially`() {
        assertFalse(upgradeSystem.hasPendingOptions())
    }

    @Test
    fun `hasPendingOptions returns true after generation`() {
        upgradeSystem.generateUpgradeOptions(state)
        assertTrue(upgradeSystem.hasPendingOptions())
    }

    @Test
    fun `clearPendingOptions clears options`() {
        upgradeSystem.generateUpgradeOptions(state)
        upgradeSystem.clearPendingOptions()
        assertFalse(upgradeSystem.hasPendingOptions())
    }

    @Test
    fun `getPendingOptions returns current options`() {
        val options = upgradeSystem.generateUpgradeOptions(state)
        assertEquals(options, upgradeSystem.getPendingOptions())
    }

    // ─── Extra Weapon Slot Impact ────────────────────────────────────

    @Test
    fun `options respect extra weapon slot for 5th weapon`() {
        // Fill 4 weapons
        state.addWeapon("pulse_cannon")
        state.addWeapon("scatter_shot")
        state.addWeapon("railgun")
        state.addWeapon("homing_missiles")

        // Add extra weapon slot
        state.addPassive("extra_weapon_slot")

        // Now should be able to get new weapons
        val available = upgradeSystem.getAvailableUpgrades(state)
        val newWeapons = available.filter {
            it.isWeapon && state.getWeaponLevel(it.id) == 0
        }

        assertTrue("Should offer new weapons with extra slot", newWeapons.isNotEmpty())
    }

    @Test
    fun `options respect reduced passive slots with extra weapon slot`() {
        // Add extra weapon slot (reduces passive slots to 3; does NOT consume a displayed slot)
        state.addPassive("extra_weapon_slot")

        // Fill all 3 real passive slots
        state.addPassive("nano_repair")
        state.addPassive("magnet_field")
        state.addPassive("vampiric_core")

        // Should not be able to add new passives
        assertFalse(state.canAddNewPassive())

        val available = upgradeSystem.getAvailableUpgrades(state)
        val newPassives = available.filter {
            !it.isWeapon && state.getPassiveStacks(it.id) == 0
        }

        assertTrue("Should not offer new passives when reduced slots full", newPassives.isEmpty())
    }

    // ─── New Item Guarantee ──────────────────────────────────────────

    @Test
    fun `generateUpgradeOptions guarantees at least one new item when available`() {
        // Add one weapon (so there are still new items available)
        state.addWeapon("pulse_cannon")

        // Run multiple times to verify the guarantee
        repeat(20) {
            val options = upgradeSystem.generateUpgradeOptions(state)

            // At least one option should be for something we don't own
            val hasNew = options.any { option ->
                !option.isEvolution && !option.isFallback &&
                if (option.isWeapon) {
                    state.getWeaponLevel(option.id) == 0
                } else {
                    state.getPassiveStacks(option.id) == 0
                }
            }

            assertTrue("Should guarantee at least one new item", hasNew)
        }
    }

    @Test
    fun `generateUpgradeOptions only offers unlocked weapons`() {
        upgradeSystem.unlockedWeaponIds = setOf("pulse_cannon", "scatter_shot")
        upgradeSystem.unlockedPassiveIds = setOf("nano_repair")

        repeat(20) {
            val options = upgradeSystem.generateUpgradeOptions(state)
            for (opt in options) {
                if (opt.isWeapon && !opt.isEvolution && !opt.isFallback) {
                    assertTrue("Weapon ${opt.id} should be unlocked",
                        opt.id in upgradeSystem.unlockedWeaponIds)
                }
                if (!opt.isWeapon && !opt.isEvolution && !opt.isFallback) {
                    assertTrue("Passive ${opt.id} should be unlocked",
                        opt.id in upgradeSystem.unlockedPassiveIds)
                }
            }
        }
    }

    @Test
    fun `generateWeaponOnlyOptions only offers unlocked weapons`() {
        upgradeSystem.unlockedWeaponIds = setOf("pulse_cannon", "railgun")

        val options = upgradeSystem.generateWeaponOnlyOptions(state)
        for (opt in options) {
            assertTrue("Weapon ${opt.id} should be unlocked",
                opt.id in upgradeSystem.unlockedWeaponIds)
        }
        assertTrue("Should have options", options.isNotEmpty())
    }

    @Test
    fun `getAvailableUpgrades respects weapon gating`() {
        upgradeSystem.unlockedWeaponIds = setOf("pulse_cannon")
        upgradeSystem.unlockedPassiveIds = setOf("nano_repair")

        val available = upgradeSystem.getAvailableUpgrades(state)
        val weaponIds = available.filter { it.isWeapon }.map { it.id }
        val passiveIds = available.filter { !it.isWeapon }.map { it.id }

        assertTrue("Should only contain pulse_cannon", weaponIds.all { it == "pulse_cannon" })
        assertTrue("Should only contain nano_repair", passiveIds.all { it == "nano_repair" })
    }

    @Test
    fun `generateUpgradeOptions always includes weapon and passive when both available`() {
        repeat(200) {
            state.reset()
            val options = upgradeSystem.generateUpgradeOptions(state)
            if (options.size >= 2) {
                val weapons = options.filter { it.isWeapon && !it.isEvolution }
                val passives = options.filter { !it.isWeapon && !it.isFallback }
                assertTrue("Must have at least one weapon", weapons.isNotEmpty())
                assertTrue("Must have at least one passive", passives.isNotEmpty())
            }
        }
    }
}

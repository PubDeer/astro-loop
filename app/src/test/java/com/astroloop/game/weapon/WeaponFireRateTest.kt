package com.astroloop.game.weapon

import com.astroloop.game.weapon.weapons.*
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies baseCooldown, baseDamage, and beatPhaseOffsetMs for all 12 weapons
 * after the DPS-neutral fire rate changes for 120 BPM beat sync.
 */
class WeaponFireRateTest {

    // ── NeedleGun: 0.25s / 5 dmg / phase 0 ─────────────────────

    @Test
    fun `NeedleGun baseCooldown is 0_25`() {
        val w = NeedleGun()
        assertEquals(0.25f, w.baseCooldown, 0.001f)
    }

    @Test
    fun `NeedleGun baseDamage is 5`() {
        val w = NeedleGun()
        assertEquals(5f, w.baseDamage, 0.001f)
    }

    @Test
    fun `NeedleGun beatPhaseOffsetMs is 0`() {
        val w = NeedleGun()
        assertEquals(0L, w.beatPhaseOffsetMs)
    }

    // ── PulseCannon: 0.5s / 15 dmg / phase 0 ───────────────────

    @Test
    fun `PulseCannon baseCooldown is 0_5`() {
        val w = PulseCannon()
        assertEquals(0.5f, w.baseCooldown, 0.001f)
    }

    @Test
    fun `PulseCannon baseDamage is 15`() {
        val w = PulseCannon()
        assertEquals(15f, w.baseDamage, 0.001f)
    }

    @Test
    fun `PulseCannon beatPhaseOffsetMs is 0`() {
        val w = PulseCannon()
        assertEquals(0L, w.beatPhaseOffsetMs)
    }

    // ── FlakCannon: 1.0s / 44 dmg / phase 0 ────────────────────

    @Test
    fun `FlakCannon baseCooldown is 1_0`() {
        val w = FlakCannon()
        assertEquals(1.0f, w.baseCooldown, 0.001f)
    }

    @Test
    fun `FlakCannon baseDamage is 44`() {
        val w = FlakCannon()
        assertEquals(44f, w.baseDamage, 0.001f)
    }

    @Test
    fun `FlakCannon beatPhaseOffsetMs is 0`() {
        val w = FlakCannon()
        assertEquals(0L, w.beatPhaseOffsetMs)
    }

    // ── ScatterShot: 1.0s / 10 dmg / phase 500 ─────────────────

    @Test
    fun `ScatterShot baseCooldown is 1_0`() {
        val w = ScatterShot()
        assertEquals(1.0f, w.baseCooldown, 0.001f)
    }

    @Test
    fun `ScatterShot baseDamage is 10`() {
        val w = ScatterShot()
        assertEquals(10f, w.baseDamage, 0.001f)
    }

    @Test
    fun `ScatterShot beatPhaseOffsetMs is 500`() {
        val w = ScatterShot()
        assertEquals(500L, w.beatPhaseOffsetMs)
    }

    // ── HomingMissiles: 1.0s / 35 dmg / phase 250 ──────────────

    @Test
    fun `HomingMissiles baseCooldown is 1_0`() {
        val w = HomingMissiles()
        assertEquals(1.0f, w.baseCooldown, 0.001f)
    }

    @Test
    fun `HomingMissiles baseDamage is 35`() {
        val w = HomingMissiles()
        assertEquals(35f, w.baseDamage, 0.001f)
    }

    @Test
    fun `HomingMissiles beatPhaseOffsetMs is 250`() {
        val w = HomingMissiles()
        assertEquals(250L, w.beatPhaseOffsetMs)
    }

    // ── Railgun: 1.5s / 80 dmg / phase 0 ──────────────────────

    @Test
    fun `Railgun baseCooldown is 1_5`() {
        val w = Railgun()
        assertEquals(1.5f, w.baseCooldown, 0.001f)
    }

    @Test
    fun `Railgun baseDamage is 80`() {
        val w = Railgun()
        assertEquals(80f, w.baseDamage, 0.001f)
    }

    @Test
    fun `Railgun beatPhaseOffsetMs is 0`() {
        val w = Railgun()
        assertEquals(0L, w.beatPhaseOffsetMs)
    }

    // ── SolarStorm: 2.0s / 29 dmg / phase 500 ──────────────────

    @Test
    fun `SolarStorm baseCooldown is 2_0`() {
        val w = SolarStorm()
        assertEquals(2.0f, w.baseCooldown, 0.001f)
    }

    @Test
    fun `SolarStorm baseDamage is 29`() {
        val w = SolarStorm()
        assertEquals(29f, w.baseDamage, 0.001f)
    }

    @Test
    fun `SolarStorm beatPhaseOffsetMs is 500`() {
        val w = SolarStorm()
        assertEquals(500L, w.beatPhaseOffsetMs)
    }

    // ── ClusterBomb: 2.0s / 60 dmg / phase 1000 ────────────────

    @Test
    fun `ClusterBomb baseCooldown is 2_0`() {
        val w = ClusterBomb()
        assertEquals(2.0f, w.baseCooldown, 0.001f)
    }

    @Test
    fun `ClusterBomb baseDamage is 60`() {
        val w = ClusterBomb()
        assertEquals(60f, w.baseDamage, 0.001f)
    }

    @Test
    fun `ClusterBomb beatPhaseOffsetMs is 1000`() {
        val w = ClusterBomb()
        assertEquals(1000L, w.beatPhaseOffsetMs)
    }

    // ── SpaceMines: 2.0s / 60 dmg / phase 1500 ─────────────────

    @Test
    fun `SpaceMines baseCooldown is 2_0`() {
        val w = SpaceMines()
        assertEquals(2.0f, w.baseCooldown, 0.001f)
    }

    @Test
    fun `SpaceMines baseDamage is 60`() {
        val w = SpaceMines()
        assertEquals(60f, w.baseDamage, 0.001f)
    }

    @Test
    fun `SpaceMines beatPhaseOffsetMs is 1500`() {
        val w = SpaceMines()
        assertEquals(1500L, w.beatPhaseOffsetMs)
    }

    // ── IonOrbiters: 4.0s / 19 dmg / phase 0 ───────────────────

    @Test
    fun `IonOrbiters baseCooldown is 4_0`() {
        val w = IonOrbiters()
        assertEquals(4.0f, w.baseCooldown, 0.001f)
    }

    @Test
    fun `IonOrbiters baseDamage is 19`() {
        val w = IonOrbiters()
        assertEquals(19f, w.baseDamage, 0.001f)
    }

    @Test
    fun `IonOrbiters beatPhaseOffsetMs is 0`() {
        val w = IonOrbiters()
        assertEquals(0L, w.beatPhaseOffsetMs)
    }

    // ── NovaBlast: 4.0s / 40 dmg / phase 2000 ──────────────────

    @Test
    fun `NovaBlast baseCooldown is 4_0`() {
        val w = NovaBlast()
        assertEquals(4.0f, w.baseCooldown, 0.001f)
    }

    @Test
    fun `NovaBlast baseDamage is 40`() {
        val w = NovaBlast()
        assertEquals(40f, w.baseDamage, 0.001f)
    }

    @Test
    fun `NovaBlast beatPhaseOffsetMs is 2000`() {
        val w = NovaBlast()
        assertEquals(2000L, w.beatPhaseOffsetMs)
    }

    // ── baseProjectileCount spec anchors ────────────────────────

    @Test fun `PulseCannon baseProjectileCount is 1`() = assertEquals(1, PulseCannon().baseProjectileCount)
    @Test fun `ScatterShot baseProjectileCount is 5`() = assertEquals(5, ScatterShot().baseProjectileCount)
    @Test fun `NeedleGun baseProjectileCount is 3`() = assertEquals(3, NeedleGun().baseProjectileCount)
    @Test fun `HomingMissiles baseProjectileCount is 1`() = assertEquals(1, HomingMissiles().baseProjectileCount)

    // ── evolution baseProjectileCount — no regression vs base L5 ─

    @Test fun `LeechBurst baseProjectileCount is 13`() =
        assertEquals(13, LeechBurst().baseProjectileCount)
    @Test fun `AutonomousAce baseProjectileCount is 5`() =
        assertEquals(5, AutonomousAce().baseProjectileCount)
    @Test fun `JackpotMines baseProjectileCount is 3`() =
        assertEquals(3, JackpotMines().baseProjectileCount)
    @Test fun `SiphonNeedles baseProjectileCount is 7`() =
        assertEquals(7, SiphonNeedles().baseProjectileCount)

    // ── EnergySaw: unchanged (excluded from beat sync) ──────────

    @Test
    fun `EnergySaw baseCooldown unchanged`() {
        val w = EnergySaw()
        assertEquals(0.1f, w.baseCooldown, 0.001f)
    }

    @Test
    fun `EnergySaw baseDamage unchanged`() {
        val w = EnergySaw()
        assertEquals(8f, w.baseDamage, 0.001f)
    }

    @Test
    fun `EnergySaw beatPhaseOffsetMs is 0`() {
        val w = EnergySaw()
        assertEquals(0L, w.beatPhaseOffsetMs)
    }
}

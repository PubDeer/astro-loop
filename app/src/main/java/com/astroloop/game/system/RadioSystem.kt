package com.astroloop.game.system

import com.astroloop.game.core.GameState
import com.astroloop.game.core.SoundManager
import com.astroloop.game.data.LoopDefinitions
import com.astroloop.game.data.PilotDefinitions
import com.astroloop.game.data.RadioDefinitions
import com.astroloop.game.data.CorruptedCrewDefinitions
import com.astroloop.game.entity.Boss
import kotlin.random.Random

class RadioSystem {

    // Boss chatter state (fires 8s into Astro corruption run)
    private var bossChatterTimer = 0f
    private var bossChatterPending = false
    private var pendingBossChatterLine = ""
    private val recentLines = RecentLineTracker()

    companion object {
        const val DISPLAY_DURATION = 4.0f
        const val FADE_DURATION = 0.2f
        const val PILOT_COOLDOWN_MIN = 10f
        const val PILOT_COOLDOWN_MAX = 15f
        const val CORRUPTED_COOLDOWN = 30f
        const val CORRUPTED_TRIGGER_CHANCE = 0.50f
        const val CHANCE_TRIGGER_PROBABILITY = 0.30f
    }

    fun update(deltaTime: Float, state: GameState) {
        // Tick cooldowns
        if (state.pilotRadioCooldown > 0f) state.pilotRadioCooldown -= deltaTime
        if (state.corruptedRadioCooldown > 0f) state.corruptedRadioCooldown -= deltaTime
        if (state.nearMissCooldown > 0f) state.nearMissCooldown -= deltaTime

        // Boss chatter: fires 8s into Astro corruption run
        if (bossChatterPending && state.isCorruptionRun) {
            bossChatterTimer += deltaTime
            if (bossChatterTimer >= 8f) {
                bossChatterPending = false
                showMessage(state, "BOSS", pendingBossChatterLine, Boss.CORRUPTION_COLOR, isCorrupted = true)
            }
        }

        // Tick display timer
        if (state.radioMessage != null) {
            if (state.radioTimer > 0f) {
                state.radioTimer -= deltaTime
            } else if (state.radioFadeTimer > 0f) {
                state.radioFadeTimer -= deltaTime
            } else {
                state.radioMessage = null
                state.radioSpeaker = null
                state.radioBoss = false
            }
        }
    }

    fun showScriptedMessage(state: GameState, speaker: String, text: String, color: Int,
                            isBoss: Boolean = false, isCorrupted: Boolean = false,
                            isGhost: Boolean = false) {
        if (isCorrupted) {
            SoundManager.playSFX("sfx_radio_corrupted")
        }
        // Non-corrupted radio lines play no sting (sfx_radio_crackle removed by request).
        state.radioMessage = text
        state.radioSpeaker = speaker
        state.radioColor = color
        state.radioTimer = DISPLAY_DURATION
        state.radioFadeTimer = 0f
        state.radioBoss = isBoss
        state.radioIsCorrupted = isCorrupted
        state.radioIsGhost = isGhost
    }

    fun showMessage(state: GameState, speaker: String, text: String, color: Int, isCorrupted: Boolean = false) {
        // The reckoning is fully scripted: ambient chatter dies at this single choke point so
        // no trigger path can leak (the fight's lines travel via showScriptedMessage instead).
        if (state.reckoningActive) return
        if (isCorrupted) {
            SoundManager.playSFX("sfx_radio_corrupted")
        }
        // Non-corrupted radio lines play no sting (sfx_radio_crackle removed by request).
        state.radioMessage = text
        state.radioSpeaker = speaker
        state.radioColor = color
        state.radioTimer = DISPLAY_DURATION
        state.radioFadeTimer = FADE_DURATION
        state.radioIsCorrupted = isCorrupted
        state.radioIsGhost = false   // ambient lines are never ghosts — never inherit a stale flag
    }

    // --- Always triggers (bypass cooldown for phoenix, boss, evolution) ---

    fun onShieldsDown(state: GameState) {
        if (state.isCorruptionRun) return
        if (state.shieldsDownTriggered) return
        state.shieldsDownTriggered = true
        val line = getLine(state, "shields_down") ?: return
        val pilot = PilotDefinitions.getPilot(state.activePilotId) ?: return
        showMessage(state, pilot.callsign, line, pilot.color)
        state.pilotRadioCooldown = randomCooldown()
    }

    fun onBigHit(state: GameState) {
        if (state.isCorruptionRun) return
        if (state.pilotRadioCooldown > 0f) return
        val line = getLine(state, "big_hit") ?: return
        val pilot = PilotDefinitions.getPilot(state.activePilotId) ?: return
        showMessage(state, pilot.callsign, line, pilot.color)
        state.pilotRadioCooldown = randomCooldown()
    }

    fun onLowHealth(state: GameState) {
        if (state.isCorruptionRun) return
        if (state.pilotRadioCooldown > 0f) return
        val line = getLine(state, "low_health") ?: return
        val pilot = PilotDefinitions.getPilot(state.activePilotId) ?: return
        showMessage(state, pilot.callsign, line, pilot.color)
        state.pilotRadioCooldown = randomCooldown()
    }

    fun onPhoenixActivate(state: GameState) {
        if (state.isCorruptionRun) return
        val line = getLine(state, "phoenix") ?: return
        val pilot = PilotDefinitions.getPilot(state.activePilotId) ?: return
        showMessage(state, pilot.callsign, line, pilot.color)
        state.pilotRadioCooldown = randomCooldown()
    }

    fun onRetreat(state: GameState) {
        val line = getLine(state, "retreat_home") ?: return
        val pilot = PilotDefinitions.getPilot(state.activePilotId) ?: return
        showMessage(state, pilot.callsign, line, pilot.color)
        state.pilotRadioCooldown = randomCooldown()
    }

    fun onBossSpawn(state: GameState) {
        if (state.isCorruptionRun) return
        val line = getLine(state, "boss_spawn") ?: return
        val pilot = PilotDefinitions.getPilot(state.activePilotId) ?: return
        showMessage(state, pilot.callsign, line, pilot.color)
        state.pilotRadioCooldown = randomCooldown()
    }

    fun onEvolution(state: GameState) {
        if (state.isCorruptionRun) return
        val line = getLine(state, "evolution") ?: return
        val pilot = PilotDefinitions.getPilot(state.activePilotId) ?: return
        showMessage(state, pilot.callsign, line, pilot.color)
        state.pilotRadioCooldown = randomCooldown()
    }

    fun onCombatStart(state: GameState) {
        recentLines.reset()
        if (state.isCorruptionRun) {
            // Corruption runs: one line from CorruptedCrewDefinitions at game start
            val corrupted = CorruptedCrewDefinitions.pilots.find { it.mirrorsPilotId == state.activePilotId }
            if (corrupted != null && corrupted.lines.isNotEmpty()) {
                val line = corrupted.lines.random()
                val pilot = PilotDefinitions.getPilot(state.activePilotId) ?: return
                showMessage(state, pilot.callsign, line, pilot.color, isCorrupted = true)
            }
            // Astro corruption run: queue a grief-stage boss chatter line for 8s in
            if (state.activePilotId == "pilot_astro") {
                val lines = LoopDefinitions.bossChatterLines(state.storyLoop)
                if (lines.isNotEmpty()) {
                    pendingBossChatterLine = lines.random()
                    bossChatterTimer = 0f
                    bossChatterPending = true
                }
            } else {
                bossChatterPending = false  // cancel any stale pending from a previous Astro run
            }
            return
        }
        val line = getLine(state, "combat_start") ?: return
        val pilot = PilotDefinitions.getPilot(state.activePilotId) ?: return
        showMessage(state, pilot.callsign, line, pilot.color)
        state.pilotRadioCooldown = randomCooldown()
    }

    // --- Chance triggers (30% probability + cooldown check) ---

    fun onFirstWeapon(state: GameState) {
        if (state.isCorruptionRun) return
        if (state.pilotRadioCooldown > 0f) return
        if (Random.nextFloat() > CHANCE_TRIGGER_PROBABILITY) return
        val line = getLine(state, "first_weapon") ?: return
        val pilot = PilotDefinitions.getPilot(state.activePilotId) ?: return
        showMessage(state, pilot.callsign, line, pilot.color)
        state.pilotRadioCooldown = randomCooldown()
    }

    fun onWeaponMaxed(state: GameState) {
        if (state.isCorruptionRun) return
        if (state.pilotRadioCooldown > 0f) return
        if (Random.nextFloat() > CHANCE_TRIGGER_PROBABILITY) return
        val line = getLine(state, "weapon_maxed") ?: return
        val pilot = PilotDefinitions.getPilot(state.activePilotId) ?: return
        showMessage(state, pilot.callsign, line, pilot.color)
        state.pilotRadioCooldown = randomCooldown()
    }

    fun onPassiveMaxed(state: GameState) {
        if (state.isCorruptionRun) return
        if (state.pilotRadioCooldown > 0f) return
        if (Random.nextFloat() > CHANCE_TRIGGER_PROBABILITY) return
        val line = getLine(state, "passive_maxed") ?: return
        val pilot = PilotDefinitions.getPilot(state.activePilotId) ?: return
        showMessage(state, pilot.callsign, line, pilot.color)
        state.pilotRadioCooldown = randomCooldown()
    }

    fun onFirstEnemy(state: GameState) {
        if (state.isCorruptionRun) return
        if (state.pilotRadioCooldown > 0f) return
        if (Random.nextFloat() > CHANCE_TRIGGER_PROBABILITY) return
        val line = getLine(state, "first_enemy") ?: return
        val pilot = PilotDefinitions.getPilot(state.activePilotId) ?: return
        showMessage(state, pilot.callsign, line, pilot.color)
        state.pilotRadioCooldown = randomCooldown()
    }

    fun onDensitySpike(state: GameState) {
        if (state.isCorruptionRun) return
        if (state.pilotRadioCooldown > 0f) return
        if (Random.nextFloat() > CHANCE_TRIGGER_PROBABILITY) return
        val line = getLine(state, "density_spike") ?: return
        val pilot = PilotDefinitions.getPilot(state.activePilotId) ?: return
        showMessage(state, pilot.callsign, line, pilot.color)
        state.pilotRadioCooldown = randomCooldown()
    }

    fun onYenMilestone(state: GameState) {
        if (state.isCorruptionRun) return
        if (state.pilotRadioCooldown > 0f) return
        if (Random.nextFloat() > CHANCE_TRIGGER_PROBABILITY) return
        val line = getLine(state, "yen_milestone") ?: return
        val pilot = PilotDefinitions.getPilot(state.activePilotId) ?: return
        showMessage(state, pilot.callsign, line, pilot.color)
        state.pilotRadioCooldown = randomCooldown()
    }

    // --- New contextual triggers ---

    fun onNearMiss(state: GameState) {
        if (state.isCorruptionRun) return
        val line = getLine(state, "near_miss") ?: return
        val pilot = PilotDefinitions.getPilot(state.activePilotId) ?: return
        showMessage(state, pilot.callsign, line, pilot.color)
        state.pilotRadioCooldown = randomCooldown()
    }

    fun onKillStreak(state: GameState) {
        if (state.isCorruptionRun) return
        if (state.pilotRadioCooldown > 0f) return
        if (Random.nextFloat() > CHANCE_TRIGGER_PROBABILITY) return
        val line = getLine(state, "kill_streak") ?: return
        val pilot = PilotDefinitions.getPilot(state.activePilotId) ?: return
        showMessage(state, pilot.callsign, line, pilot.color)
        state.pilotRadioCooldown = randomCooldown()
    }

    fun onTimeMilestone(state: GameState, milestoneIndex: Int) {
        if (state.isCorruptionRun && !state.astroLoopMode) return
        if (milestoneIndex >= 4 && !state.astroLoopMode) return
        val event = when (milestoneIndex) {
            0 -> "time_2min"
            1 -> "time_4min"
            2 -> "time_6min"
            3 -> "time_8min"
            4 -> if (state.astroLoopMode && !state.activePilotHasBandana) "astro_echo" else "time_10min"
            5 -> "time_12min"
            6 -> "time_14min"
            7 -> "time_16min"
            else -> return
        }
        val line = getLine(state, event) ?: return
        val pilot = PilotDefinitions.getPilot(state.activePilotId) ?: return
        showMessage(state, pilot.callsign, line, pilot.color)
        state.pilotRadioCooldown = randomCooldown()
    }

    // --- Corrupted crew (independent cooldown) ---

    fun onEnemySpawn(state: GameState) {
        if (state.isCorruptionRun) return
        if (state.corruptedRadioCooldown > 0f) return
        if (Random.nextFloat() > CORRUPTED_TRIGGER_CHANCE) return

        val pilots = CorruptedCrewDefinitions.pilots
        var index = Random.nextInt(pilots.size)
        if (index == state.lastCorruptedPilotIndex && pilots.size > 1) {
            index = (index + 1) % pilots.size
        }
        state.lastCorruptedPilotIndex = index

        val corrupted = pilots[index]
        val line = corrupted.lines.random()
        val mirrorPilot = PilotDefinitions.getPilot(corrupted.mirrorsPilotId)
        val speakerCallsign = mirrorPilot?.callsign ?: "???"
        showMessage(state, speakerCallsign, line, 0xFF886666.toInt(), isCorrupted = true)
        state.corruptedRadioCooldown = CORRUPTED_COOLDOWN
    }

    private fun getLine(state: GameState, eventType: String): String? {
        val filter = state.droneDeparted || state.astroLoopMode
        val pid = state.activePilotId
        if (state.isPostHorrorRun) {
            val horrorPool = RadioDefinitions.getLines(pid, "${eventType}_horror", filter)
            if (horrorPool != null) return recentLines.pick("$pid:${eventType}_horror", horrorPool)
        }
        val pool = RadioDefinitions.getLines(pid, eventType, filter) ?: return null
        return recentLines.pick("$pid:$eventType", pool)
    }

    private fun randomCooldown(): Float {
        return PILOT_COOLDOWN_MIN + Random.nextFloat() * (PILOT_COOLDOWN_MAX - PILOT_COOLDOWN_MIN)
    }
}

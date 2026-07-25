package com.astroloop.game.data

import android.content.Context
import com.astroloop.game.core.GameState
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TelemetryManager(context: Context) {

    private val telemetryFile = File(context.filesDir, "telemetry.json")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    private val runs = mutableListOf<JSONObject>()
    private val purchases = mutableListOf<JSONObject>()
    private val casinoSpins = mutableListOf<JSONObject>()

    private var currentRun: JSONObject? = null
    private val currentSnapshots = mutableListOf<JSONObject>()
    private val currentUpgradeChoices = mutableListOf<JSONObject>()

    init {
        loadFromDisk()
    }

    fun logRunStart(ship: String, pilot: String, storeUpgrades: Map<String, Int>) {
        currentRun = JSONObject().apply {
            put("startTime", dateFormat.format(Date()))
            put("ship", ship)
            put("pilot", pilot)
            val upgObj = JSONObject()
            for ((k, v) in storeUpgrades) upgObj.put(k, v)
            put("storeUpgrades", upgObj)
        }
        currentSnapshots.clear()
        currentUpgradeChoices.clear()
    }

    fun logSnapshot(
        state: GameState,
        damageByWeapon: Map<String, Float>,
        damageTakenBy: Map<String, Float>,
        critsThisMinute: Int,
        powerupsCollected: Map<String, Int>,
        dodges: Int
    ) {
        val snap = JSONObject().apply {
            put("timeSec", state.survivalTime.toInt())
            put("health", state.formatTime(state.survivalTime))
            put("difficulty", String.format("%.2f", state.difficultyMultiplier))
            put("weapons", mapToJson(state.weaponLevels))
            put("passives", mapToJson(state.passiveStacks))
            if (damageByWeapon.isNotEmpty()) put("dmgByWeapon", floatMapToJson(damageByWeapon))
            if (damageTakenBy.isNotEmpty()) put("dmgTakenBy", floatMapToJson(damageTakenBy))
            if (critsThisMinute > 0) put("crits", critsThisMinute)
            if (powerupsCollected.isNotEmpty()) put("powerups", mapToJson(powerupsCollected))
            if (dodges > 0) put("dodges", dodges)
        }
        currentSnapshots.add(snap)
    }

    fun logUpgradeOffered(timeSec: Int, options: List<String>, chosen: String) {
        val entry = JSONObject().apply {
            put("timeSec", timeSec)
            put("options", JSONArray(options))
            put("chosen", chosen)
        }
        currentUpgradeChoices.add(entry)
    }

    fun logRunEnd(
        causeOfDeath: String,
        survivedSeconds: Float,
        yenEarned: Int,
        yenFromAsteroids: Int,
        yenFromEnemies: Int,
        enemiesKilled: Int,
        asteroidsDestroyed: Int,
        totalDamageDealt: Float,
        totalDamageTaken: Float,
        critsTotal: Int,
        upgradeDrops: Int,
        diamonds: Int,
        finalWeapons: Map<String, Int>,
        finalPassives: Map<String, Int>,
        evolutionsTriggered: List<String>
    ) {
        val run = currentRun ?: return
        run.put("endTime", dateFormat.format(Date()))
        run.put("causeOfDeath", causeOfDeath)
        run.put("survivedSec", survivedSeconds.toInt())
        run.put("yenEarned", yenEarned)
        run.put("yenFromAsteroids", yenFromAsteroids)
        run.put("yenFromEnemies", yenFromEnemies)
        run.put("enemiesKilled", enemiesKilled)
        run.put("asteroidsDestroyed", asteroidsDestroyed)
        run.put("totalDmgDealt", totalDamageDealt.toInt())
        run.put("totalDmgTaken", totalDamageTaken.toInt())
        run.put("critsTotal", critsTotal)
        run.put("upgradeDrops", upgradeDrops)
        run.put("diamonds", diamonds)
        run.put("finalWeapons", mapToJson(finalWeapons))
        run.put("finalPassives", mapToJson(finalPassives))
        if (evolutionsTriggered.isNotEmpty()) run.put("evolutions", JSONArray(evolutionsTriggered))
        if (currentSnapshots.isNotEmpty()) run.put("snapshots", JSONArray(currentSnapshots.map { it }))
        if (currentUpgradeChoices.isNotEmpty()) run.put("upgrades", JSONArray(currentUpgradeChoices.map { it }))

        runs.add(run)
        currentRun = null
        currentSnapshots.clear()
        currentUpgradeChoices.clear()
    }

    fun logPurchase(type: String, item: String, level: Int, cost: Int, yenAfter: Int) {
        val entry = JSONObject().apply {
            put("time", dateFormat.format(Date()))
            put("type", type)
            put("item", item)
            put("level", level)
            put("cost", cost)
            put("yenAfter", yenAfter)
        }
        purchases.add(entry)
        saveToDisk()
    }

    fun logCasinoSpin(result: List<String>, payout: Int, yenAfter: Int) {
        val entry = JSONObject().apply {
            put("time", dateFormat.format(Date()))
            put("reels", JSONArray(result))
            put("payout", payout)
            put("yenAfter", yenAfter)
        }
        casinoSpins.add(entry)
        saveToDisk()
    }

    fun clearLog() {
        runs.clear()
        purchases.clear()
        casinoSpins.clear()
        currentRun = null
        currentSnapshots.clear()
        currentUpgradeChoices.clear()
        telemetryFile.delete()
    }

    fun flush() {
        saveToDisk()
    }

    fun getRunCount(): Int = runs.size

    fun getFileSizeKB(): Float {
        return if (telemetryFile.exists()) telemetryFile.length() / 1024f else 0f
    }

    fun getRunSummaries(): List<String> {
        return runs.mapIndexed { i, run ->
            val ship = run.optString("ship", "?").removePrefix("ship_")
            val pilot = run.optString("pilot", "?").removePrefix("pilot_")
            val sec = run.optInt("survivedSec", 0)
            val mins = sec / 60
            val secs = sec % 60
            val kills = run.optInt("enemiesKilled", 0)
            val yen = run.optInt("yenEarned", 0)
            "#${i + 1} $ship/$pilot ${mins}:${String.format("%02d", secs)} ${kills}k ${yen}Y"
        }
    }

    fun getRunDetail(index: Int): List<String> {
        if (index < 0 || index >= runs.size) return emptyList()
        val run = runs[index]
        val lines = mutableListOf<String>()

        val cause = run.optString("causeOfDeath", "?")
        lines.add("Death: $cause")

        val dmgDealt = run.optInt("totalDmgDealt", 0)
        val dmgTaken = run.optInt("totalDmgTaken", 0)
        lines.add("Dmg dealt: $dmgDealt | taken: $dmgTaken")

        val yenA = run.optInt("yenFromAsteroids", 0)
        val yenE = run.optInt("yenFromEnemies", 0)
        lines.add("Yen: asteroids=$yenA enemies=$yenE")

        val crits = run.optInt("critsTotal", 0)
        val drops = run.optInt("upgradeDrops", 0)
        val diamonds = run.optInt("diamonds", 0)
        lines.add("Crits: $crits | Drops: $drops | Dia: $diamonds")

        val weapons = run.optJSONObject("finalWeapons")
        if (weapons != null) {
            val wList = weapons.keys().asSequence().map { "${it}L${weapons.getInt(it)}" }.joinToString(", ")
            lines.add("W: $wList")
        }

        val passives = run.optJSONObject("finalPassives")
        if (passives != null) {
            val pList = passives.keys().asSequence().map { "${it}x${passives.getInt(it)}" }.joinToString(", ")
            lines.add("P: $pList")
        }

        return lines
    }

    // --- File I/O ---

    private fun loadFromDisk() {
        if (!telemetryFile.exists()) return
        try {
            val json = JSONObject(telemetryFile.readText())
            jsonArrayToList(json.optJSONArray("runs")).forEach { runs.add(it) }
            jsonArrayToList(json.optJSONArray("purchases")).forEach { purchases.add(it) }
            jsonArrayToList(json.optJSONArray("casinoSpins")).forEach { casinoSpins.add(it) }
        } catch (_: Exception) {
            // Corrupt file — start fresh
        }
    }

    private fun saveToDisk() {
        try {
            val json = JSONObject().apply {
                put("version", 1)
                put("runs", JSONArray(runs.map { it }))
                put("purchases", JSONArray(purchases.map { it }))
                put("casinoSpins", JSONArray(casinoSpins.map { it }))
            }
            val tmp = File(telemetryFile.parent, "telemetry.json.tmp")
            tmp.writeText(json.toString(2))
            tmp.renameTo(telemetryFile)
        } catch (_: Exception) {
            // Best-effort write
        }
    }

    // --- Helpers ---

    private fun mapToJson(map: Map<String, Int>): JSONObject {
        val obj = JSONObject()
        for ((k, v) in map) obj.put(k, v)
        return obj
    }

    private fun floatMapToJson(map: Map<String, Float>): JSONObject {
        val obj = JSONObject()
        for ((k, v) in map) obj.put(k, v.toInt())
        return obj
    }

    private fun jsonArrayToList(arr: JSONArray?): List<JSONObject> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { arr.getJSONObject(it) }
    }
}

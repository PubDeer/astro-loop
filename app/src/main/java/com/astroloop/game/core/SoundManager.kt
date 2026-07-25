package com.astroloop.game.core

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log

/**
 * Singleton managing all game audio.
 *
 * - SoundPool for short one-shot SFX (low latency, concurrent playback)
 * - MediaPlayer for ambient loops (crossfading between tracks)
 * - Data-driven: sound files in res/raw/ named to match event IDs.
 *   Missing files = silence, no code changes needed to add/remove sounds.
 * - Volume categories: ambient, sfx_combat, sfx_ui, sfx_radio — independent multipliers
 * - Respects system volume — in-app mute toggle via setMuted()
 * - Combat BGM: sequential segment playback (intro → loop) with BeatClock sync
 */
object SoundManager {

    private const val TAG = "SoundManager"
    private const val MAX_STREAMS = 16
    private const val CROSSFADE_STEP = 0.05f

    // ── Volume category multipliers (0.0–1.0) ──────────────────────────
    // Per-sound loudness is baked into the files (LUFS ladder); these category knobs sit
    // near 1.0 so we don't double-attenuate the normalized assets.
    var volumeAmbient: Float = 0.8f      // was 0.25 — music is now baked to ~−18 LUFS
    var volumeSfxCombat: Float = 1.0f
    var volumeSfxUi: Float = 0.8f        // was 0.25 — UI baked to −18 LUFS
    var volumeSfxRadio: Float = 0.8f     // was 0.25 — radio baked to −18 LUFS

    private const val DEBUG_BGM_VOLUME = 0.25f
    // The menu tap is mixed hot relative to the other UI sounds — trim it to 30%.
    private const val TAP_VOLUME_MULT = 0.3f

    var activeSet: String = "normal"
    var weaponVolume: Float = 1.0f

    // (weaponSfxVolume map removed — per-weapon loudness is now baked into the normalized
    //  files on the LUFS ladder; getWeaponSfxVolume returns a flat 1.0.)

    // Per-weapon beat timing offset (ms, additive to beatPhaseOffsetMs)
    // Per-weapon attack-delay compensation. Reset to empty: the old values were tuned by ear for
    // the pre-instrument-restyle sounds, and the current SFX are export-trimmed so their transient
    // sits at t=0 (~5 ms) — they should fire right on the grid boundary (offset 0). Re-tune here
    // only if a specific new sound's perceived attack reads early/late.
    private val weaponBeatOffsetMs = emptyMap<String, Long>()

    // Per-weapon balance now lives in the normalized files (−15 LUFS), not here.
    fun getWeaponSfxVolume(weaponId: String): Float = 1.0f
    fun getWeaponBeatOffsetMs(weaponId: String): Long = weaponBeatOffsetMs[weaponId] ?: 0L

    val beatClock = BeatClock(120f)
    // Wall-clock time the combat loop actually started — the beat grid anchors here (the music's
    // downbeat), NOT game-load time, so weapons land on the music's beat even though the track
    // started earlier during the launch animation.
    private var combatMusicStartMs = 0L

    var isMuted: Boolean = false
        private set

    // ── Debug BGM state (tuning page) ──────────────────────────────────
    private var bgmMutedForDebug = false
    private var debugBgmPlayer: MediaPlayer? = null

    private val weaponCooldownMs = mapOf(
        "energy_saw" to 100L, "needle_gun" to 250L, "pulse_cannon" to 500L,
        "flak_cannon" to 1000L, "scatter_shot" to 1000L, "homing_missiles" to 1000L,
        "railgun" to 2000L, "solar_storm" to 2000L, "cluster_bomb" to 2000L,
        "space_mines" to 2000L, "nova_blast" to 4000L,
        // Evolutions (inherit base weapon cooldown)
        "storm_cannon" to 500L, "warp_saw" to 100L, "leech_burst" to 1000L,
        "autonomous_ace" to 1000L, "oblivion_beam" to 2000L,
        "jackpot_mines" to 2000L, "phoenix_flare" to 2000L, "lingering_nova" to 4000L,
        "siphon_needles" to 250L, "hunter_killer" to 2000L, "flak_barrage" to 1000L
    )
    private val weaponPhaseOffsetMs = mapOf(
        "scatter_shot" to 500L, "homing_missiles" to 250L,
        "solar_storm" to 500L, "cluster_bomb" to 1000L,
        "space_mines" to 1500L, "nova_blast" to 2000L
    )

    // ── Internal state ─────────────────────────────────────────────────
    private var initialized = false
    private var appContext: Context? = null

    // SoundPool for one-shot SFX
    private var soundPool: SoundPool? = null
    private val sfxIds = mutableMapOf<String, Int>()       // eventId → SoundPool sound id
    private val sfxLoaded = mutableSetOf<Int>()             // sound ids confirmed loaded
    // Pending plays: queued when playSFX is called before a sound finishes loading
    private data class PendingPlay(val volume: Float, val rate: Float, val categoryVolume: Float)
    private val pendingPlays = mutableMapOf<Int, PendingPlay>()  // soundId → play params

    // Ambient MediaPlayers (dual-player for gapless looping)
    private var ambientCurrent: MediaPlayer? = null
    private var ambientNext: MediaPlayer? = null
    private var ambientFadingOut: MediaPlayer? = null
    private var currentAmbientId: String? = null

    // Combat BGM sequential segments
    private var combatPlayer: MediaPlayer? = null
    private var combatSegments = listOf<String>()
    private var currentSegmentIndex = 0
    private var combatActive = false

    // Boss fight BGM
    private var bossPlayer: MediaPlayer? = null

    // Boss/reckoning BGM target level. setMuted and the reckoning duck both go through
    // this so a mute/unmute during the ducked ghost scene can't snap the volume back up.
    private var bossPlayerVolume = 0.7f

    // Intro swell — a ~10s musical sting. Played via MediaPlayer, not SoundPool: SoundPool
    // caps each sample at ~1MB of decoded PCM (~5s at 48kHz stereo) and silently truncates
    // the rest, which cut the swell off mid-fade on the launchpad.
    private var introSwellPlayer: MediaPlayer? = null

    // ── All known SFX event IDs ────────────────────────────────────────
    private val allSfxEventIds = listOf(
        // Hangar (loaded first — launch is the earliest possible player action)
        // NOTE: sfx_intro_swell is intentionally NOT here — it's ~10s, which exceeds
        // SoundPool's ~1MB sample cap. It plays via MediaPlayer (playIntroSwell).
        "sfx_launch",
        "sfx_slot_win", "sfx_pilot_recruit",
        // Radio
        "sfx_radio_corrupted",
        // UI
        "sfx_ui_tap", "sfx_ui_swipe", "sfx_ui_purchase",
        "sfx_ui_upgrade_select", "sfx_slot_spin", "sfx_slot_jackpot",
        // Combat
        "sfx_shield_hit", "sfx_shield_break", "sfx_player_hit", "sfx_player_death",
        "sfx_boss_spawn", "sfx_phoenix_revive", "sfx_crystal_activate", "sfx_evolution",
        "sfx_lucky_star_bounce", "sfx_lucky_star_select",
        // Base weapons
        "sfx_weapon_pulse_cannon", "sfx_weapon_energy_saw", "sfx_weapon_scatter_shot",
        "sfx_weapon_homing_missiles", "sfx_weapon_ion_orbiters", "sfx_weapon_railgun",
        "sfx_weapon_space_mines", "sfx_weapon_solar_storm", "sfx_weapon_nova_blast",
        "sfx_weapon_needle_gun", "sfx_weapon_cluster_bomb", "sfx_weapon_flak_cannon",
        // Evolution weapons
        "sfx_weapon_storm_cannon", "sfx_weapon_warp_saw", "sfx_weapon_leech_burst",
        "sfx_weapon_autonomous_ace", "sfx_weapon_frost_ring", "sfx_weapon_oblivion_beam",
        "sfx_weapon_jackpot_mines", "sfx_weapon_phoenix_flare", "sfx_weapon_lingering_nova",
        "sfx_weapon_siphon_needles", "sfx_weapon_hunter_killer", "sfx_weapon_flak_barrage",
        // Drone
        "sfx_weapon_drone",
        // Extra
        "sfx_enemy_death", "sfx_asteroid_break", "sfx_powerup_pickup", "sfx_near_miss",
        "sfx_explosion", "sfx_yen_pickup",
        // Narrative
        "sfx_text_tick",
        // Desert
        "sfx_tank_shot", "sfx_desert_enemy_gun",
        // Crystal reveal + fleet
        "sfx_crystal_glow", "sfx_crystal_orb",
        // Crystal reckoning (finale events)
        "sfx_reckoning_bullet",
        "sfx_ghost_lance", "sfx_crystal_shatter"
    )

    // ── Volume category routing ────────────────────────────────────────

    private fun getVolumeCategory(eventId: String): Float {
        return when {
            eventId.startsWith("sfx_radio") -> volumeSfxRadio
            eventId.startsWith("sfx_ui_") || eventId.startsWith("sfx_slot") -> volumeSfxUi
            else -> volumeSfxCombat
        }
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
        if (muted) {
            setPlayerVolume(ambientCurrent, 0f)
            setPlayerVolume(ambientNext, 0f)
            setPlayerVolume(ambientFadingOut, 0f)
            setPlayerVolume(combatPlayer, 0f)
            bossPlayer?.setVolume(0f, 0f)
            setPlayerVolume(debugBgmPlayer, 0f)
        } else {
            setPlayerVolume(ambientCurrent, volumeAmbient)
            setPlayerVolume(ambientNext, volumeAmbient)
            setPlayerVolume(combatPlayer, volumeAmbient)
            bossPlayer?.setVolume(bossPlayerVolume, bossPlayerVolume)
            setPlayerVolume(debugBgmPlayer, DEBUG_BGM_VOLUME)
        }
    }

    // ── Debug BGM controls (tuning page) ────────────────────────────────

    fun muteBGMForDebug() {
        bgmMutedForDebug = true
        combatPlayer?.let { setPlayerVolume(it, 0f) }
        ambientCurrent?.let { setPlayerVolume(it, 0f) }
        ambientNext?.let { setPlayerVolume(it, 0f) }
    }

    fun unmuteBGMForDebug() {
        bgmMutedForDebug = false
        combatPlayer?.let { setPlayerVolume(it, if (isMuted) 0f else volumeAmbient) }
        ambientCurrent?.let { setPlayerVolume(it, if (isMuted) 0f else volumeAmbient) }
        ambientNext?.let { setPlayerVolume(it, if (isMuted) 0f else volumeAmbient) }
    }

    fun playDebugBGM(context: Context, setName: String) {
        stopDebugBGM()
        val resName = "bgm_${setName}_combat_loop"
        val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
        if (resId == 0) { Log.d(TAG, "Debug BGM not found: $resName"); return }
        try {
            debugBgmPlayer = MediaPlayer.create(context, resId)?.apply {
                isLooping = true
                val vol = if (isMuted) 0f else DEBUG_BGM_VOLUME
                setVolume(vol, vol)
                start()
            }
            beatClock.start(System.currentTimeMillis())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play debug BGM: $resName", e)
        }
    }

    fun stopDebugBGM() {
        debugBgmPlayer?.let { fadeOutAndRelease(it) }
        debugBgmPlayer = null
        beatClock.stop()
    }

    // ── Tuning page auto-play ─────────────────────────────────────────
    // weaponId → Pair(volButtonIndex 0-10, offsetButtonIndex 0-10)
    val tuningAutoPlay = mutableMapOf<String, Pair<Int, Int>>()
    private val tuningNextFire = mutableMapOf<String, Long>()

    private val TUNING_VOL_MULTIPLIERS = floatArrayOf(
        0.50f, 0.60f, 0.70f, 0.80f, 0.90f, 1.00f, 1.10f, 1.20f, 1.40f, 1.60f, 2.00f
    )
    private val TUNING_OFFSET_MS = longArrayOf(
        -125L, -100L, -75L, -50L, -25L, 0L, 25L, 50L, 75L, 100L, 125L
    )

    fun getTuningVolume(buttonIndex: Int): Float = 0.5f * TUNING_VOL_MULTIPLIERS[buttonIndex.coerceIn(0, 10)]
    fun getTuningOffsetMs(buttonIndex: Int): Long = TUNING_OFFSET_MS[buttonIndex.coerceIn(0, 10)]

    fun toggleTuningAutoPlay(weaponId: String, volIndex: Int, offsetIndex: Int) {
        if (tuningAutoPlay.containsKey(weaponId)) {
            tuningAutoPlay.remove(weaponId)
            tuningNextFire.remove(weaponId)
        } else {
            tuningAutoPlay[weaponId] = Pair(volIndex, offsetIndex)
            val cooldown = weaponCooldownMs[weaponId] ?: 500L
            val phaseOffset = weaponPhaseOffsetMs[weaponId] ?: 0L
            val timingOffset = getTuningOffsetMs(offsetIndex)
            val now = System.currentTimeMillis()
            val delay = if (beatClock.isRunning) beatClock.msUntilNextSubdivision(cooldown, now, phaseOffset) else 0L
            tuningNextFire[weaponId] = now + delay + timingOffset
        }
    }

    fun updateTuningSettings(weaponId: String, volIndex: Int, offsetIndex: Int) {
        if (tuningAutoPlay.containsKey(weaponId)) {
            val oldOffset = tuningAutoPlay[weaponId]!!.second
            tuningAutoPlay[weaponId] = Pair(volIndex, offsetIndex)
            // Recalculate next fire time when offset changes
            if (oldOffset != offsetIndex) {
                val cooldown = weaponCooldownMs[weaponId] ?: 500L
                val phaseOffset = weaponPhaseOffsetMs[weaponId] ?: 0L
                val timingOffset = getTuningOffsetMs(offsetIndex)
                val now = System.currentTimeMillis()
                val delay = if (beatClock.isRunning) beatClock.msUntilNextSubdivision(cooldown, now, phaseOffset) else 0L
                tuningNextFire[weaponId] = now + delay + timingOffset
            }
        }
    }

    fun updateTuningAutoPlay() {
        if (!beatClock.isRunning || tuningAutoPlay.isEmpty()) return
        val now = System.currentTimeMillis()
        for ((weaponId, settings) in tuningAutoPlay) {
            val nextFire = tuningNextFire[weaponId] ?: continue
            if (now >= nextFire) {
                val volume = getTuningVolume(settings.first)
                playSFX("sfx_weapon_$weaponId", volume, 1.0f, isSoundboard = true)
                val cooldown = weaponCooldownMs[weaponId] ?: 500L
                tuningNextFire[weaponId] = nextFire + cooldown
            }
        }
    }

    fun clearTuningAutoPlay() {
        tuningAutoPlay.clear()
        tuningNextFire.clear()
    }

    // ── Initialization ─────────────────────────────────────────────────

    /**
     * Initialize SoundPool and load all SFX from res/raw/.
     * Safe to call multiple times — only the first call takes effect.
     */
    fun init(ctx: Context) {
        if (initialized) return

        appContext = ctx.applicationContext

        val prefs = ctx.getSharedPreferences("astrohunt_save", Context.MODE_PRIVATE)
        isMuted = prefs.getBoolean("audio_muted", false)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(audioAttributes)
            .build()
            .also { pool ->
                pool.setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0) {
                        sfxLoaded.add(sampleId)
                        // Play any sound that was requested before it finished loading
                        pendingPlays.remove(sampleId)?.let { pending ->
                            if (!isMuted) {
                                val vol = (pending.volume * pending.categoryVolume).coerceIn(0f, 1f)
                                pool.play(sampleId, vol, vol, 1, 0, pending.rate.coerceIn(0.5f, 2.0f))
                            }
                        }
                    }
                }
            }

        initialized = true

        // Load SFX on background thread — pendingPlays queue handles early access
        Thread {
            for (eventId in allSfxEventIds) {
                loadSfx(eventId)
            }
        }.start()
    }

    /**
     * Try to load a single SFX from res/raw/. If the resource doesn't exist,
     * silently skip — no crash, no code changes needed.
     */
    private fun loadSfx(eventId: String) {
        val ctx = appContext ?: return
        val pool = soundPool ?: return

        val resId = ctx.resources.getIdentifier(eventId, "raw", ctx.packageName)
        if (resId == 0) {
            // File not in res/raw/ — that's fine, play will be silent
            return
        }

        try {
            val soundId = pool.load(ctx, resId, 1)
            sfxIds[eventId] = soundId
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load SFX: $eventId", e)
        }
    }

    // ── One-shot SFX ───────────────────────────────────────────────────

    /**
     * Fire a one-shot SFX. If the event ID has no loaded file, this is a no-op.
     *
     * @param eventId  The SFX event name (e.g. "sfx_weapon_railgun")
     * @param volume   Per-call volume multiplier (0.0–1.0), combined with category volume
     * @param rate     Playback rate (0.5–2.0)
     */
    fun playSFX(eventId: String, volume: Float = 1.0f, rate: Float = 1.0f, isSoundboard: Boolean = false) {
        if (isMuted) return
        val pool = soundPool ?: return
        val soundId = sfxIds[eventId] ?: return

        // Per-sound loudness trim applied before both the deferred and immediate paths.
        val perSoundVolume = if (eventId == "sfx_ui_tap") volume * TAP_VOLUME_MULT else volume

        val categoryVolume = getVolumeCategory(eventId)

        if (!sfxLoaded.contains(soundId)) {
            // Sound still loading — queue it to play when ready
            pendingPlays[soundId] = PendingPlay(perSoundVolume, rate, categoryVolume)
            return
        }

        val weaponMult = if (!isSoundboard && eventId.startsWith("sfx_weapon_")) weaponVolume else 1f
        val finalVolume = (perSoundVolume * categoryVolume * weaponMult).coerceIn(0f, 1f)
        pool.play(soundId, finalVolume, finalVolume, 1, 0, rate.coerceIn(0.5f, 2.0f))
    }

    // ── Ambient (single track, e.g. hangar) ────────────────────────────

    /**
     * Start or crossfade to an ambient track. The file should exist in res/raw/
     * as e.g. `ambient_hangar.ogg`. If already playing the same track, no-op.
     * If a different track is playing, crossfade out the old one.
     */
    fun playAmbient(ambientId: String) {
        if (ambientId == currentAmbientId && ambientCurrent?.isPlaying == true) return

        val ctx = appContext ?: return
        val resId = ctx.resources.getIdentifier(ambientId, "raw", ctx.packageName)
        if (resId == 0) {
            Log.d(TAG, "Ambient track not found: $ambientId")
            return
        }

        // Dequeue next player before switching tracks
        try { ambientCurrent?.setNextMediaPlayer(null) } catch (_: Exception) {}
        ambientNext?.release()
        ambientNext = null

        // Fade out current ambient if any
        ambientCurrent?.let { old ->
            ambientFadingOut?.release()
            ambientFadingOut = old
            fadeOutAndRelease(old)
        }

        // Start new ambient
        try {
            val current = MediaPlayer.create(ctx, resId) ?: return
            current.isLooping = false
            current.setVolume(0f, 0f)
            current.start()
            fadeIn(current, if (isMuted) 0f else volumeAmbient)
            ambientCurrent = current
            currentAmbientId = ambientId
            queueNextAmbient(resId)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play ambient: $ambientId", e)
        }
    }

    private fun queueNextAmbient(resId: Int) {
        val ctx = appContext ?: return
        val current = ambientCurrent ?: return
        var next: MediaPlayer? = null
        try {
            next = MediaPlayer.create(ctx, resId) ?: return
            next.isLooping = false
            current.setNextMediaPlayer(next)
            current.setOnCompletionListener { done ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    done.release()
                    if (ambientCurrent === done) {
                        ambientCurrent = next
                        ambientNext = null
                        // Apply current mute state now that next has become current
                        setPlayerVolume(ambientCurrent, if (isMuted) 0f else volumeAmbient)
                        val id = currentAmbientId ?: return@post
                        val nextResId = ctx.resources.getIdentifier(id, "raw", ctx.packageName)
                        if (nextResId != 0) queueNextAmbient(nextResId)
                    }
                }
            }
            ambientNext = next
        } catch (e: Exception) {
            Log.w(TAG, "Failed to queue next ambient", e)
            next?.release()
        }
    }

    /**
     * Stop the current ambient track with a fade-out.
     */
    fun stopAmbient(fadeOutMillis: Long? = null) {
        try { ambientCurrent?.setNextMediaPlayer(null) } catch (_: Exception) {}
        ambientNext?.release()
        ambientNext = null
        val toFade = ambientCurrent
        ambientCurrent = null
        currentAmbientId = null
        toFade?.let { fadeOutAndRelease(it, fadeOutMillis = fadeOutMillis) }
    }

    // ── Combat BGM (sequential segments) ───────────────────────────────

    /**
     * Start combat BGM. Plays segments sequentially (intro → loop).
     * The last segment loops. BeatClock starts for weapon sync.
     */
    /**
     * Start combat music early (during launch animation) with a fade-in.
     * BeatClock is NOT started — call startCombatMusic() later for beat sync.
     */
    fun startCombatMusicEarly(context: Context) {
        if (combatActive) return
        stopAmbient()
        combatActive = true
        combatSegments = listOf("bgm_${activeSet}_combat_loop")
        currentSegmentIndex = 0
        playCombatSegment(context, 0)
        combatMusicStartMs = System.currentTimeMillis()   // anchor ≈ music onset (after prep/start)
        // Override volume — fade in from silence
        combatPlayer?.let {
            it.setVolume(0f, 0f)
            fadeIn(it, if (isMuted) 0f else volumeAmbient)
        }
    }

    fun startCombatMusic(context: Context) {
        if (combatActive) {
            // Music already started early (launch animation) — anchor the beat grid to WHEN THE
            // MUSIC STARTED, not now, so weapon hits land on the music's beat (not offset by the
            // launch-animation gap). The clock's elapsed-since-start math handles the catch-up.
            beatClock.start(combatMusicStartMs)
            return
        }
        stopAmbient()
        combatActive = true
        combatSegments = listOf("bgm_${activeSet}_combat_loop")
        currentSegmentIndex = 0
        playCombatSegment(context, 0)
        combatMusicStartMs = System.currentTimeMillis()
        beatClock.start(combatMusicStartMs)
    }

    private fun playCombatSegment(context: Context, index: Int) {
        combatPlayer?.release()
        val resId = context.resources.getIdentifier(combatSegments[index], "raw", context.packageName)
        if (resId == 0) {
            Log.d(TAG, "Combat segment not found: ${combatSegments[index]}")
            return
        }
        try {
            combatPlayer = MediaPlayer.create(context, resId)?.apply {
                val isLastSegment = index == combatSegments.size - 1
                isLooping = isLastSegment
                val vol = if (isMuted) 0f else volumeAmbient
                setVolume(vol, vol)
                setOnCompletionListener {
                    if (!isLastSegment && combatActive) {
                        currentSegmentIndex = index + 1
                        playCombatSegment(context, currentSegmentIndex)
                    }
                }
                start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play combat segment: ${combatSegments[index]}", e)
        }
    }

    /**
     * Stop combat BGM with fade-out. Stops BeatClock.
     */
    fun stopCombatMusic() {
        combatActive = false
        beatClock.stop()
        combatPlayer?.let { fadeOutAndRelease(it) }
        combatPlayer = null
        bossPlayer?.let { fadeOutAndRelease(it) }
        bossPlayer = null
    }

    fun startBossBGM(context: Context) = startBossTrack(context, "bgm_boss")

    /** Crystal Reckoning BGM — same player lifecycle as the boss track, its own bake. */
    fun startReckoningBGM(context: Context) = startBossTrack(context, "bgm_reckoning")

    private fun startBossTrack(context: Context, resName: String) {
        // This can run on a boss-spawn frame before any fight phase is set, and GameThread
        // swallows anything update() throws — a throw here permanently freezes the scripted
        // choreography. MediaPlayer.create(context, 0) throws NotFoundException, so a
        // missing track must bail out first, before killing the combat loop.
        val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
        if (resId == 0) {
            Log.w(TAG, "$resName not found in res/raw — keeping the combat loop")
            return
        }

        // Fade out combat music if still playing
        combatPlayer?.let { fadeOutAndRelease(it) }
        combatPlayer = null
        combatActive = false

        bossPlayer?.release()
        bossPlayerVolume = 0.7f
        val vol = if (isMuted) 0f else bossPlayerVolume
        try {
            bossPlayer = MediaPlayer.create(context, resId)?.apply {
                isLooping = true
                setVolume(vol, vol)
                start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play $resName", e)
        }
        // Re-lock the beat grid to the track's start so grid consumers stay on-beat.
        beatClock.start(System.currentTimeMillis())
    }

    /**
     * Duck the boss/reckoning BGM to an absolute level — the reckoning win strips to the
     * bare bed under the ghost dialogue. Sticky across mute/unmute via bossPlayerVolume.
     */
    fun duckBossBGM(volume: Float) {
        bossPlayerVolume = volume
        bossPlayer?.let { setPlayerVolume(it, volume) }
    }

    /** Fade the boss/reckoning BGM out and release it — the shatter kills the beat. */
    fun fadeOutBossBGM() {
        bossPlayer?.let { fadeOutAndRelease(it, fromVolume = bossPlayerVolume) }
        bossPlayer = null
    }

    /**
     * Play the intro swell (one-shot). Uses MediaPlayer so the full ~10s plays —
     * SoundPool would truncate it at ~5s (see introSwellPlayer). Full volume to match
     * its previous SoundPool playback (volumeSfxCombat = 1.0).
     */
    fun playIntroSwell(context: Context) {
        if (isMuted) return
        introSwellPlayer?.release()
        introSwellPlayer = null
        val resId = context.resources.getIdentifier("sfx_intro_swell", "raw", context.packageName)
        if (resId == 0) return
        try {
            introSwellPlayer = MediaPlayer.create(context, resId)?.apply {
                isLooping = false
                setVolume(1f, 1f)
                setOnCompletionListener {
                    it.release()
                    if (introSwellPlayer === it) introSwellPlayer = null
                }
                start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play intro swell", e)
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────────────

    /**
     * Pause all audio (call from Activity.onPause).
     */
    fun pause() {
        ambientCurrent?.let { if (it.isPlaying) it.pause() }
        // Prevent automatic transition to ambientNext while backgrounded
        try { ambientCurrent?.setNextMediaPlayer(null) } catch (_: Exception) {}
        combatPlayer?.let { if (it.isPlaying) it.pause() }
        bossPlayer?.let { if (it.isPlaying) it.pause() }
        debugBgmPlayer?.let { if (it.isPlaying) it.pause() }
        introSwellPlayer?.let { if (it.isPlaying) it.pause() }
        soundPool?.autoPause()
    }

    /**
     * Resume all audio (call from Activity.onResume).
     */
    fun resume() {
        ambientCurrent?.start()
        // Release stale next player before re-queuing (pause dequeued it via setNextMediaPlayer(null))
        ambientNext?.release()
        ambientNext = null
        // Re-queue next player
        val id = currentAmbientId
        val ctx = appContext
        if (id != null && ctx != null) {
            val resId = ctx.resources.getIdentifier(id, "raw", ctx.packageName)
            if (resId != 0) queueNextAmbient(resId)
        }
        if (combatActive) combatPlayer?.start()
        bossPlayer?.start()
        debugBgmPlayer?.start()
        try { introSwellPlayer?.start() } catch (_: Exception) {}
        soundPool?.autoResume()
    }

    /**
     * Stop all audio immediately (no fade).
     */
    fun stopAll() {
        stopAmbient()
        stopCombatMusic()
        stopDebugBGM()
        clearTuningAutoPlay()
        introSwellPlayer?.let { fadeOutAndRelease(it, fromVolume = 1f, fadeOutMillis = 400) }
        introSwellPlayer = null
        // SoundPool streams stop when pool is paused/destroyed
        soundPool?.autoPause()
        volumeAmbient = 0.8f
    }

    /**
     * Release all resources. Call from Activity.onDestroy.
     * After this, init() must be called again before use.
     */
    fun release() {
        try { ambientCurrent?.setNextMediaPlayer(null) } catch (_: Exception) {}
        ambientCurrent?.release()
        ambientCurrent = null
        ambientNext?.release()
        ambientNext = null
        ambientFadingOut?.release()
        ambientFadingOut = null
        currentAmbientId = null

        combatPlayer?.release()
        combatPlayer = null
        combatActive = false

        bossPlayer?.release()
        bossPlayer = null

        introSwellPlayer?.release()
        introSwellPlayer = null

        debugBgmPlayer?.release()
        debugBgmPlayer = null
        bgmMutedForDebug = false
        clearTuningAutoPlay()

        soundPool?.release()
        soundPool = null
        sfxIds.clear()
        sfxLoaded.clear()
        pendingPlays.clear()

        initialized = false
        appContext = null
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun setPlayerVolume(player: MediaPlayer?, volume: Float) {
        try {
            val v = if (isMuted) 0f else volume.coerceIn(0f, 1f)
            player?.setVolume(v, v)
        } catch (e: IllegalStateException) {
            // Player may have been released
        }
    }

    /**
     * Fade in a MediaPlayer from 0 to target volume over ~500ms.
     * Uses a background thread to avoid blocking the game loop.
     */
    private fun fadeIn(player: MediaPlayer, targetVolume: Float) {
        Thread {
            var vol = 0f
            try {
                while (vol < targetVolume) {
                    vol = (vol + CROSSFADE_STEP).coerceAtMost(targetVolume)
                    player.setVolume(vol, vol)
                    Thread.sleep(20)
                }
            } catch (_: Exception) {
                // Player released during fade — ignore
            }
        }.start()
    }

    /**
     * Fade out a MediaPlayer and release it when done.
     * Uses a background thread to avoid blocking the game loop.
     */
    private fun fadeOutAndRelease(
        player: MediaPlayer,
        fromVolume: Float = volumeAmbient,
        fadeOutMillis: Long? = null,
    ) {
        Thread {
            try {
                val start = if (isMuted) 0f else fromVolume
                val step = if (fadeOutMillis != null)
                    start / (fadeOutMillis / 20L).coerceAtLeast(1L)
                else CROSSFADE_STEP
                var vol = start
                while (vol > 0f) {
                    vol = (vol - step).coerceAtLeast(0f)
                    player.setVolume(vol, vol)
                    Thread.sleep(20)
                }
                player.stop()
                player.release()
            } catch (_: Exception) {
                // Player may already be released — ignore
                try { player.release() } catch (_: Exception) {}
            }
        }.start()
    }
}

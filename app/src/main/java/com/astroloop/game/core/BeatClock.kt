package com.astroloop.game.core

class BeatClock(val bpm: Float) {
    val beatIntervalMs: Long = (60_000L / bpm).toLong()

    private var startTimeMs: Long = 0L
    private var pauseTimeMs: Long = 0L
    var isRunning: Boolean = false
        private set

    fun start(currentTimeMs: Long) {
        startTimeMs = currentTimeMs
        isRunning = true
    }

    fun stop() {
        isRunning = false
    }

    fun pause(currentTimeMs: Long) {
        pauseTimeMs = currentTimeMs
    }

    fun resumeFromPause(currentTimeMs: Long) {
        if (pauseTimeMs > 0L) {
            startTimeMs += (currentTimeMs - pauseTimeMs)
            pauseTimeMs = 0L
        }
    }

    fun subdivisionMs(beats: Float): Long = (beats * beatIntervalMs).toLong()

    fun msUntilNextSubdivision(subdivisionMs: Long, currentTimeMs: Long, phaseOffsetMs: Long = 0L): Long {
        val elapsed = currentTimeMs - startTimeMs
        if (elapsed < phaseOffsetMs) return phaseOffsetMs - elapsed
        val sincePhase = elapsed - phaseOffsetMs
        val remainder = sincePhase % subdivisionMs
        return if (remainder == 0L) 0L else subdivisionMs - remainder
    }

    /**
     * Delay for re-anchoring a repeating shot to the grid at fire time (needle family):
     * always schedules a tick strictly after now. Unlike [msUntilNextSubdivision],
     * landing exactly on a tick yields a full subdivision (that tick's shot just
     * fired), and landing in the first half of a subdivision — only possible when a
     * frame hitch pushed the shot almost a full tick late — bumps past the imminent
     * tick so a single tick never fires twice.
     */
    fun gridAnchoredDelayMs(subdivisionMs: Long, currentTimeMs: Long, phaseOffsetMs: Long = 0L): Long {
        val raw = msUntilNextSubdivision(subdivisionMs, currentTimeMs, phaseOffsetMs)
        return if (raw < subdivisionMs / 2) raw + subdivisionMs else raw
    }

    fun elapsedBeats(currentTimeMs: Long): Float {
        return (currentTimeMs - startTimeMs).toFloat() / beatIntervalMs.toFloat()
    }

    fun elapsedMs(currentTimeMs: Long): Long = currentTimeMs - startTimeMs

    companion object {
        fun cooldownToSubdivision(cooldownSeconds: Float, bpm: Float): Float {
            val beatInterval = 60f / bpm
            return cooldownSeconds / beatInterval
        }
    }
}

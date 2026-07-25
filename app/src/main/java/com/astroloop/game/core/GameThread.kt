package com.astroloop.game.core

import android.graphics.Canvas
import android.view.SurfaceHolder

class GameThread(
    private val surfaceHolder: SurfaceHolder,
    private val gameView: GameSurfaceView
) : Thread() {

    @Volatile
    var isRunning: Boolean = false
        private set

    private var lastFrameTime: Long = 0
    private var frameCount: Int = 0
    private var lastFpsTime: Long = 0
    @Volatile
    var currentFps: Int = 0
        private set

    fun setRunning(running: Boolean) {
        isRunning = running
    }

    override fun run() {
        var canvas: Canvas?

        lastFrameTime = System.nanoTime()
        lastFpsTime = System.currentTimeMillis()

        while (isRunning) {
            canvas = null

            // Capture frame start time BEFORE canvas lock
            val frameStart = System.nanoTime()
            val deltaTime = (frameStart - lastFrameTime) / 1_000_000_000f
            lastFrameTime = frameStart
            val clampedDelta = deltaTime.coerceAtMost(0.033f)

            try {
                canvas = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    try {
                        surfaceHolder.lockHardwareCanvas()
                    } catch (e: Exception) {
                        surfaceHolder.lockCanvas()
                    }
                } else {
                    surfaceHolder.lockCanvas()
                }

                if (canvas != null) {
                    synchronized(surfaceHolder) {
                        gameView.update(clampedDelta)
                        gameView.render(canvas)

                        // FPS counter
                        frameCount++
                        val now = System.currentTimeMillis()
                        if (now - lastFpsTime >= 1000) {
                            currentFps = frameCount
                            frameCount = 0
                            lastFpsTime = now
                        }
                    }
                }
            } catch (e: Throwable) {
                // Catch Throwable (not just Exception) so a per-frame Error — e.g.
                // OutOfMemoryError during a heavy scene's bitmap rebuild on resume, or
                // a recycled-bitmap draw racing surfaceChanged — skips the frame and is
                // logged instead of killing the process. The next frame can recover.
                e.printStackTrace()
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // Sleep for remainder of frame budget, measured from frameStart
            val elapsed = (System.nanoTime() - frameStart) / 1_000_000
            val targetFrameTime = GameConfig.FRAME_TIME_MS
            if (elapsed < targetFrameTime) {
                try {
                    sleep(targetFrameTime - elapsed)
                } catch (e: InterruptedException) {
                    // Ignore
                }
            }
        }
    }

    fun pause() {
        isRunning = false
        interrupt()          // wake the thread immediately if it's sleeping
        try {
            join(1000)       // increased from 500 ms
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun resumeThread() {
        // Thread will be restarted by GameSurfaceView
    }
}

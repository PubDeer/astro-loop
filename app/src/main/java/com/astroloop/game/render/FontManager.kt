package com.astroloop.game.render

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.astroloop.game.R

/**
 * Singleton manager for custom fonts.
 * Body family = Exo 2 (getRegular/getBold), the default for all game text.
 * Display family = Orbitron (getDisplayRegular/getDisplayBold), for the
 * pause overlays (death crystals are wordless).
 * Call initialize() from MainActivity.onCreate() BEFORE any renderer is
 * constructed — renderer Paint fields capture the typeface at construction.
 */
object FontManager {

    private var bodyRegular: Typeface? = null
    private var bodyBold: Typeface? = null
    private var displayRegular: Typeface? = null
    private var displayBold: Typeface? = null
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        try {
            bodyRegular = ResourcesCompat.getFont(context, R.font.exo2_regular)
            bodyBold = ResourcesCompat.getFont(context, R.font.exo2_bold)
            displayRegular = ResourcesCompat.getFont(context, R.font.orbitron_regular)
            displayBold = ResourcesCompat.getFont(context, R.font.orbitron_bold)
            initialized = true
        } catch (e: Exception) {
            // Fallback to monospace if font loading fails
            bodyRegular = Typeface.MONOSPACE
            bodyBold = Typeface.MONOSPACE
            displayRegular = Typeface.MONOSPACE
            displayBold = Typeface.MONOSPACE
            initialized = true
        }
    }

    fun getRegular(): Typeface = bodyRegular ?: Typeface.MONOSPACE
    fun getBold(): Typeface = bodyBold ?: Typeface.MONOSPACE
    fun getDisplayRegular(): Typeface = displayRegular ?: Typeface.MONOSPACE
    fun getDisplayBold(): Typeface = displayBold ?: Typeface.MONOSPACE
}

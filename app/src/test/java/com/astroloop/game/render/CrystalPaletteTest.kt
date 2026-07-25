package com.astroloop.game.render

import org.junit.Assert.*
import org.junit.Test

class CrystalPaletteTest {
    @Test fun hasFiveLayerColors() {
        assertEquals(5, CrystalPalette.LAYER_COLORS.size)
    }

    @Test fun layerColorsAreTheColdSpectrumInOrder() {
        assertEquals(0xFF88EEFF.toInt(), CrystalPalette.LAYER_COLORS[0]) // cyan
        assertEquals(0xFF5FA8FF.toInt(), CrystalPalette.LAYER_COLORS[1]) // blue
        assertEquals(0xFF8F7FFF.toInt(), CrystalPalette.LAYER_COLORS[2]) // indigo
        assertEquals(0xFFD07FFF.toInt(), CrystalPalette.LAYER_COLORS[3]) // violet
        assertEquals(0xFFE8F6FF.toInt(), CrystalPalette.LAYER_COLORS[4]) // white
    }

    @Test fun p1MatchesTheCrystalsOwnMidColor() {
        // The base drip IS the crystal's ambient colour — they must never drift apart.
        assertEquals(CrystalPalette.MID, CrystalPalette.LAYER_COLORS[0])
    }

    @Test fun everyLayerColorIsDistinct() {
        assertEquals(5, CrystalPalette.LAYER_COLORS.toSet().size)
    }
}

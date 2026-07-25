package com.astroloop.game.render

import org.junit.Assert.assertEquals
import org.junit.Test

class IconCacheTest {

    @Test
    fun `pilotIdToFilename strips pilot_ prefix`() {
        assertEquals("portrait_medic",    IconCache.pilotIdToFilename("pilot_medic"))
        assertEquals("portrait_rascal",   IconCache.pilotIdToFilename("pilot_rascal"))
        assertEquals("portrait_astro",    IconCache.pilotIdToFilename("pilot_astro"))
        assertEquals("portrait_unit7",    IconCache.pilotIdToFilename("pilot_unit7"))
    }

    @Test
    fun `pilotIdToFilename handles tb26 special case`() {
        assertEquals("portrait_tb26", IconCache.pilotIdToFilename("tb26"))
    }

    @Test
    fun `storeIdToFilename maps all upgrade IDs`() {
        assertEquals("salvage_plate",  IconCache.storeIdToFilename("health"))
        assertEquals("deflector_rig",  IconCache.storeIdToFilename("shields"))
        assertEquals("nitro_boost",    IconCache.storeIdToFilename("speed"))
        assertEquals("hot_rounds",     IconCache.storeIdToFilename("damage"))
        assertEquals("lucky_rounds",   IconCache.storeIdToFilename("crit"))
        assertEquals("haul_line",      IconCache.storeIdToFilename("magnet"))
        assertEquals("finders_fee",    IconCache.storeIdToFilename("yen_bonus"))
        assertEquals("scavenger_rig",  IconCache.storeIdToFilename("salvage"))
        assertEquals("time_crystal",   IconCache.storeIdToFilename(null))
    }

    @Test
    fun `slotSymbolToFilename maps all slot constants`() {
        assertEquals("yen",     IconCache.slotSymbolToFilename(0))
        assertEquals("star",    IconCache.slotSymbolToFilename(1))
        assertEquals("diamond", IconCache.slotSymbolToFilename(2))
        assertEquals("rocket",  IconCache.slotSymbolToFilename(3))
        assertEquals("bolt",    IconCache.slotSymbolToFilename(4))
        assertEquals("gear",    IconCache.slotSymbolToFilename(5))
    }
}

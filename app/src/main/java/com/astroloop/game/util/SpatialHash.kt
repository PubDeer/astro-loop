package com.astroloop.game.util

import com.astroloop.game.entity.Entity

class SpatialHash(
    private val cellSize: Float = 100f
) {
    private val cells = HashMap<Long, MutableList<Entity>>()
    private val entityCells = HashMap<Entity, MutableList<Long>>()

    private val queryResultList = ArrayList<Entity>(64)
    private val querySeenSet = HashSet<Entity>(64)

    fun clear() {
        cells.clear()
        entityCells.clear()
    }

    fun insert(entity: Entity) {
        val cellKeys = getCellsForEntity(entity)
        entityCells[entity] = cellKeys.toMutableList()

        for (key in cellKeys) {
            cells.getOrPut(key) { mutableListOf() }.add(entity)
        }
    }

    fun remove(entity: Entity) {
        entityCells[entity]?.forEach { key ->
            cells[key]?.remove(entity)
        }
        entityCells.remove(entity)
    }

    fun update(entity: Entity) {
        remove(entity)
        insert(entity)
    }

    fun query(x: Float, y: Float, radius: Float): List<Entity> {
        queryResultList.clear()
        querySeenSet.clear()

        val minCellX = ((x - radius) / cellSize).toInt()
        val maxCellX = ((x + radius) / cellSize).toInt()
        val minCellY = ((y - radius) / cellSize).toInt()
        val maxCellY = ((y + radius) / cellSize).toInt()

        for (cx in minCellX..maxCellX) {
            for (cy in minCellY..maxCellY) {
                val key = cellKey(cx, cy)
                val bucket = cells[key] ?: continue
                for (entity in bucket) {
                    if (querySeenSet.add(entity)) {
                        queryResultList.add(entity)
                    }
                }
            }
        }

        return queryResultList
    }

    fun queryRect(x: Float, y: Float, width: Float, height: Float): List<Entity> {
        val result = mutableSetOf<Entity>()

        val minCellX = (x / cellSize).toInt()
        val maxCellX = ((x + width) / cellSize).toInt()
        val minCellY = (y / cellSize).toInt()
        val maxCellY = ((y + height) / cellSize).toInt()

        for (cx in minCellX..maxCellX) {
            for (cy in minCellY..maxCellY) {
                val key = cellKey(cx, cy)
                cells[key]?.let { result.addAll(it) }
            }
        }

        return result.toList()
    }

    fun getPotentialCollisions(entity: Entity): List<Entity> {
        val result = mutableSetOf<Entity>()

        entityCells[entity]?.forEach { key ->
            cells[key]?.forEach { other ->
                if (other !== entity) {
                    result.add(other)
                }
            }
        }

        return result.toList()
    }

    private fun getCellsForEntity(entity: Entity): List<Long> {
        val cells = mutableListOf<Long>()

        val minCellX = ((entity.position.x - entity.radius) / cellSize).toInt()
        val maxCellX = ((entity.position.x + entity.radius) / cellSize).toInt()
        val minCellY = ((entity.position.y - entity.radius) / cellSize).toInt()
        val maxCellY = ((entity.position.y + entity.radius) / cellSize).toInt()

        for (cx in minCellX..maxCellX) {
            for (cy in minCellY..maxCellY) {
                cells.add(cellKey(cx, cy))
            }
        }

        return cells
    }

    private fun cellKey(x: Int, y: Int): Long {
        return (x.toLong() shl 32) or (y.toLong() and 0xFFFFFFFFL)
    }
}

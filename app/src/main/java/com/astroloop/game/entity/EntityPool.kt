package com.astroloop.game.entity

class EntityPool<T : Entity>(
    private val factory: () -> T,
    initialSize: Int = 50
) {
    private val available = ArrayDeque<T>()
    private val inUse = mutableListOf<T>()

    init {
        repeat(initialSize) {
            available.add(factory())
        }
    }

    fun obtain(): T {
        val entity = if (available.isNotEmpty()) {
            available.removeFirst()
        } else {
            factory()
        }
        entity.reset()
        entity.isActive = true
        inUse.add(entity)
        return entity
    }

    fun free(entity: T) {
        entity.isActive = false
        entity.reset()
        inUse.remove(entity)
        available.add(entity)
    }

    fun freeAll() {
        inUse.forEach { entity ->
            entity.isActive = false
            entity.reset()
            available.add(entity)
        }
        inUse.clear()
    }

    fun getActiveEntities(): List<T> = inUse.filter { it.isActive }

    fun getActiveEntities(result: MutableList<T>) {
        result.clear()
        for (entity in inUse) {
            if (entity.isActive) result.add(entity)
        }
    }

    fun getAllInUse(): List<T> = inUse.toList()

    fun forEach(action: (T) -> Unit) {
        inUse.forEach { if (it.isActive) action(it) }
    }

    fun removeInactive(): List<T> {
        val toRemove = inUse.filter { !it.isActive }
        toRemove.forEach { entity ->
            inUse.remove(entity)
            available.add(entity)
        }
        return toRemove
    }

    fun activeCount(): Int = inUse.count { it.isActive }

    fun totalCount(): Int = available.size + inUse.size
}

// Convenience object for managing all entity pools
object EntityPools {
    val asteroids = EntityPool({ Asteroid() }, 100)
    val projectiles = EntityPool({ Projectile() }, 200)
    val powerUps = EntityPool({ PowerUp() }, 30)
    val enemies = EntityPool({ EnemyShip() }, 10)

    fun resetAll() {
        asteroids.freeAll()
        projectiles.freeAll()
        powerUps.freeAll()
        enemies.freeAll()
    }

    fun cleanupInactive() {
        asteroids.removeInactive()
        projectiles.removeInactive()
        powerUps.removeInactive()
        enemies.removeInactive()
    }
}

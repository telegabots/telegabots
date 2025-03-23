package org.github.telegabots.api

import org.github.telegabots.api.entity.BaseEntity

/**
 * Support storing and retrieving [BaseEntity]
 */
interface EntityRepository<T : BaseEntity> : UserService {
    /**
     * Saves entity
     */
    fun save(entity: T): T

    /**
     * Gets entity by id
     */
    fun findById(id: Long): T?

    /**
     * Checks if entity exists by id
     */
    fun existsById(id: Long): Boolean

    /**
     * Deletes entity by id
     *
     * @return true if entity was actually deleted, otherwise false
     */
    fun delete(id: Long): Boolean

    /**
     * Deletes all entities
     */
    fun deleteAll(): Long

    /**
     * Counts all entities
     */
    fun count(): Long

    /**
     * Finds all entities
     */
    fun findAll(): List<T>

    /**
     * Saves all entities
     */
    fun saveAll(entities: List<T>): List<T>

    /**
     * Gets entities as page
     *
     * @param page page number, starting from 0
     * @param size page size
     */
    fun findPage(page: Int, size: Int): EntityPage<T>

    companion object {
        const val DEFAULT_PAGE_SIZE: Int = 20
    }
}

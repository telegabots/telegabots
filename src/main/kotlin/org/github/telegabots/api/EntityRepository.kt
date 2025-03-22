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
     * Loads entity by id
     */
    fun load(id: Long): T?

    /**
     * Deletes entity by id
     *
     * @return true if entity was actually deleted, otherwise false
     */
    fun delete(id: Long): Boolean
}

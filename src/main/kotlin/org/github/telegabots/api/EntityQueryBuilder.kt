package org.github.telegabots.api

import org.github.telegabots.api.annotation.Index1
import org.github.telegabots.api.annotation.Unique1
import org.github.telegabots.api.entity.BaseEntity

/**
 * Query builder for entities which can use fields annotated with [Unique1] and [Index1]
 */
interface EntityQueryBuilder<T : BaseEntity> {
    /**
     * Adds where clause to query with [Unique1] field
     */
    fun whereUnique1(value: String): EntityQueryBuilder<T>

    /**
     * Adds where clause to query with [Index1] field
     */
    fun whereIndex1(value: Long): EntityQueryBuilder<T>

    /**
     * Counts all entities by this query
     */
    fun count(): Long

    /**
     * Finds all entities by this query
     */
    fun findAll(): List<T>

    /**
     * Gets entities as page by this query
     *
     * @param page page number, starting from 0
     * @param size page size
     */
    fun findPage(page: Int, size: Int): EntityPage<T>
}

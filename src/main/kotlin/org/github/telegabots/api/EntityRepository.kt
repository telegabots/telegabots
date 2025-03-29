package org.github.telegabots.api

import org.github.telegabots.api.entity.BaseEntity
import java.util.function.Predicate

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

    /**
     * Finds entities by filter using [findPage]
     *
     * NOTE: this method is not efficient, use it only for small number of entities
     *
     * @param size number of entities to find
     * @param filter predicate to filter entities
     */
    fun findByFilter(size: Int, filter: Predicate<T>): List<T> {
        if (size <= 0) return emptyList()
        val result = ArrayList<T>(size)
        var page = findPage(0, size)

        while (page.hasContent()) {
            for (item in page.getContent().filter(filter::test)) {
                result.add(item)
                if (result.size >= size) {
                    return result
                }
            }
            if (page.isLast()) {
                break
            }
            page = findPage(page.getPage() + 1, size)
        }

        return result
    }

    /**
     * Creates a query builder
     */
    fun query(): EntityQueryBuilder<T>

    companion object {
        const val DEFAULT_PAGE_SIZE: Int = 20
    }
}

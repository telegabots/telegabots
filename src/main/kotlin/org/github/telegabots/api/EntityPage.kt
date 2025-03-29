package org.github.telegabots.api

import org.github.telegabots.api.entity.BaseEntity

/**
 * Represents a page of entities
 *
 * @param T type of entity
 */
interface EntityPage<T : BaseEntity> {
    /**
     * Returns the content of the page
     */
    fun getContent(): List<T>

    /**
     * Returns the number of the current page
     */
    fun getPage(): Int

    /**
     * Returns the size of the page
     */
    fun getSize(): Int

    /**
     * Returns the number of pages
     */
    fun getTotalPages(): Int

    /**
     * Returns the total number of pages
     */
    fun getTotalElements(): Long

    /**
     * Returns true if [getContent] is not empty
     */
    fun hasContent(): Boolean = getContent().isNotEmpty()

    /**
     * Returns true if this page is the first page
     */
    fun isFirst(): Boolean = getPage() == 0

    /**
     * Returns true if this page is the last page
     */
    fun isLast(): Boolean = getPage() == getTotalPages() - 1
}

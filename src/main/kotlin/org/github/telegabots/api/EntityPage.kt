package org.github.telegabots.api

import org.github.telegabots.api.entity.BaseEntity

interface EntityPage<T : BaseEntity> {
    /**
     * Returns the content of the page
     */
    fun getContent(): List<T>

    /**
     * Returns the number of the current page
     */
    fun getNumber(): Int

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
}

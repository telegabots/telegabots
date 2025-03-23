package org.github.telegabots.util

import org.github.telegabots.api.EntityPage
import org.github.telegabots.api.entity.BaseEntity

/**
 * Implementation of [EntityPage]
 */
internal data class EntityPageImpl<T : BaseEntity>(
    private val content: List<T>,
    private val number: Int,
    private val size: Int,
    private val totalPages: Int,
    private val totalElements: Long
) : EntityPage<T> {
    override fun getContent(): List<T> = content

    override fun getNumber(): Int = number

    override fun getSize(): Int = size

    override fun getTotalPages(): Int = totalPages

    override fun getTotalElements(): Long = totalElements
}

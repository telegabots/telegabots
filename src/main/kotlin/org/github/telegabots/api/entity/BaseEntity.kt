package org.github.telegabots.api.entity

import org.github.telegabots.api.EntityRepository

/**
 * Base entity for all user entities
 *
 * @see EntityRepository
 */
abstract class BaseEntity(private var id: Long? = null) {
    open fun getId(): Long? = id

    open fun setId(id: Long?)  {
        this.id = id
    }
}

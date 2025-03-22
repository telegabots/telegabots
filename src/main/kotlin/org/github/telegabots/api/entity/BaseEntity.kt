package org.github.telegabots.api.entity

import org.github.telegabots.api.EntityRepository

/**
 * Base entity for all user entities
 *
 * @see EntityRepository
 */
abstract class BaseEntity {
    var id: Long? = null
}

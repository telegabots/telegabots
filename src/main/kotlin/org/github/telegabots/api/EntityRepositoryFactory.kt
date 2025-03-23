package org.github.telegabots.api

import org.github.telegabots.api.entity.BaseEntity

/**
 * Factory for creating [EntityRepository] instances.
 */
interface EntityRepositoryFactory : UserService {
    fun <T : BaseEntity> getRepository(clazz: Class<T>): EntityRepository<T>
}

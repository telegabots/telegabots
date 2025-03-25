package org.github.telegabots.sqlite

import org.github.telegabots.api.EntityRepository
import org.github.telegabots.api.EntityRepositoryFactory
import org.github.telegabots.api.entity.BaseEntity
import org.github.telegabots.service.JsonService
import org.jooq.DSLContext
import java.util.concurrent.locks.ReadWriteLock

/**
 * Factory for creating [SqliteEntityRepository] instances
 */
internal class SqliteEntityRepositoryFactory(
    private val userId: Long,
    private val context: DSLContext,
    private val jsonService: JsonService,
    private val readWriteLock: ReadWriteLock
) : EntityRepositoryFactory {
    override fun userId(): Long = userId

    override fun <T : BaseEntity> getRepository(clazz: Class<T>): EntityRepository<T> {
        return SqliteEntityRepository(clazz, userId, context, jsonService, readWriteLock)
    }
}

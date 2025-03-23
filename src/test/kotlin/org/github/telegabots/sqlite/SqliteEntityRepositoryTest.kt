package org.github.telegabots.sqlite

import org.github.telegabots.api.EntityRepository
import org.github.telegabots.api.EntityRepositoryTest
import org.github.telegabots.api.entity.BaseEntity
import org.github.telegabots.service.JsonService
import org.github.telegabots.util.SqliteConnectionUtil
import org.jooq.impl.DSL
import kotlin.io.path.createTempFile

/**
 * Test [SqliteEntityRepository], implementation of [EntityRepository]
 */
class SqliteEntityRepositoryTest : EntityRepositoryTest() {

    override fun <T : BaseEntity> getRepository(
        clazz: Class<T>,
        userId: Long,
        repository: EntityRepository<*>?
    ): EntityRepository<T> {
        val dbPath = createTempFile(prefix = "test", suffix = ".db")
        val context = if (repository != null) {
            // Use the same context for all repositories
            (repository as SqliteEntityRepository).getContext()
        } else {
            DSL.using(SqliteConnectionUtil.getConnection(dbPath))
        }
        return SqliteEntityRepository(clazz, userId, context, jsonService)
    }

    private companion object {
        val jsonService = JsonService()
    }
}

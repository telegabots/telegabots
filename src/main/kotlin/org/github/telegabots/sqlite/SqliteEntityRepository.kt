package org.github.telegabots.sqlite

import org.github.telegabots.api.EntityRepository
import org.github.telegabots.api.entity.BaseEntity
import org.github.telegabots.jooq.Tables.USER_ENTITIES
import org.github.telegabots.jooq.tables.records.UserEntitiesRecord
import org.github.telegabots.service.JsonService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import java.sql.Connection

/**
 * Sqlite implementation of [EntityRepository]
 *
 * TODO: add synchronization for multi-threading
 */
internal class SqliteEntityRepository<T : BaseEntity>(
    private val entityClass: Class<out T>,
    private val userId: Long,
    private val conn: Connection,
    private val jsonService: JsonService
) : EntityRepository<T> {
    private val context: DSLContext = DSL.using(conn)

    override fun userId(): Long = userId

    override fun save(entity: T): T {
        val record = if (entity.id != null) {
            val oldRecord = getEntityRecord(entity.id!!)
                ?: error("Entity '${entityClass.name}' with id ${entity.id} not found")

            if (userId != oldRecord.userId) {
                throw IllegalArgumentException("User $userId cannot access entity ${oldRecord.id} of another user ${oldRecord.userId}")
            }
            oldRecord
        } else {
            context.newRecord(USER_ENTITIES).also {
                it.userId = this.userId
            }
        }
        record.entity = jsonService.toJson(entity)
        record.store()
        entity.id = record.id
        return entity
    }

    override fun load(id: Long): T? {
        return getEntityRecord(id)
            ?.let { toEntity(it) }
    }

    override fun delete(id: Long): Boolean {
        return getEntityRecord(id)?.let { record ->
            if (userId != record.userId) {
                throw IllegalArgumentException("User $userId cannot access entity ${record.id} of another user ${record.userId}")
            }

            context.deleteFrom(USER_ENTITIES)
                .where(USER_ENTITIES.ID.eq(id))
                .execute() > 0
        } ?: false
    }

    private fun getEntityRecord(id: Long): UserEntitiesRecord? = context.selectFrom(USER_ENTITIES)
        .where(USER_ENTITIES.ID.eq(id))
        .fetchOne()

    private fun toEntity(record: UserEntitiesRecord): T {
        if (userId != record.userId) {
            throw IllegalArgumentException("User $userId cannot access entity ${record.id} of another user ${record.userId}")
            // TODO: maybe return null instead of throwing exception?
        }

        val jsonEntity: String = record.entity
        try {
            return jsonService.parse(jsonEntity, entityClass)
        } catch (ex: Exception) {
            log.error("Failed to parse entity from json:\n$jsonEntity", ex)
            throw ex
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(SqliteEntityRepository::class.java)!!
    }
}

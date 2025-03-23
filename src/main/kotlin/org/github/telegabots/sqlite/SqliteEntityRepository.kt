package org.github.telegabots.sqlite

import org.github.telegabots.api.EntityPage
import org.github.telegabots.api.EntityRepository
import org.github.telegabots.api.entity.BaseEntity
import org.github.telegabots.jooq.Tables.ENTITY_TYPES
import org.github.telegabots.jooq.Tables.USER_ENTITIES
import org.github.telegabots.jooq.tables.records.EntityTypesRecord
import org.github.telegabots.jooq.tables.records.UserEntitiesRecord
import org.github.telegabots.service.JsonService
import org.github.telegabots.util.EntityPageImpl
import org.github.telegabots.util.TimeUtil
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.time.LocalDateTime

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
    private val entityType = getOrCreateEntityInfo()
    private val mainCondition = USER_ENTITIES.USER_ID.eq(userId).and(USER_ENTITIES.ENTITY_TYPES_ID.eq(entityType.id))

    override fun userId(): Long = userId

    override fun save(entity: T): T {
        val record = if (entity.getId() != null) {
            getEntityRecord(entity.getId()!!)
                ?: error("Entity '${entityClass.name}' with id ${entity.getId()} not found or not belongs to user")
        } else {
            context.newRecord(USER_ENTITIES).also { record ->
                record.userId = this.userId
                record.entityTypesId = entityType.id
            }
        }
        record.entity = jsonService.toJson(entity)
        record.updatedAt = TimeUtil.toEpochMillis(LocalDateTime.now())
        record.store()
        entity.setId(record.id)
        return entity
    }

    override fun findById(id: Long): T? {
        return getEntityRecord(id)?.let { record -> toEntity(record) }
    }

    override fun existsById(id: Long): Boolean {
        return context.fetchExists(USER_ENTITIES.where(mainCondition.and(USER_ENTITIES.ID.eq(id))))
    }

    override fun delete(id: Long): Boolean {
        return context.deleteFrom(USER_ENTITIES)
            .where(mainCondition.and(USER_ENTITIES.ID.eq(id)))
            .execute() > 0
    }

    override fun deleteAll(): Long {
        return context.deleteFrom(USER_ENTITIES)
            .where(mainCondition)
            .execute()
            .toLong()
    }

    override fun count(): Long =
        context.fetchCount(USER_ENTITIES.where(mainCondition)).toLong()

    override fun findAll(): List<T> {
        return context.selectFrom(USER_ENTITIES)
            .where(mainCondition)
            .fetch()
            .map { record -> toEntity(record) }
    }

    override fun findPage(page: Int, size: Int): EntityPage<T> {
        val size = if (size <= 0) EntityRepository.DEFAULT_PAGE_SIZE else size
        val offset = page * size
        val list = context.selectFrom(USER_ENTITIES)
            .where(mainCondition)
            .offset(offset.toLong())
            .limit(size)
            .fetch()

        val totalElements = count()
        val totalPages = if (totalElements == 0L) 0 else (totalElements - 1) / size + 1

        return EntityPageImpl(
            content = list.map { record -> toEntity(record) },
            number = page,
            size = size,
            totalPages = totalPages.toInt(),
            totalElements = totalElements
        )
    }

    override fun saveAll(entities: List<T>): List<T> {
        return entities.map { save(it) }
    }

    fun getConnection(): Connection = conn

    private fun getEntityRecord(id: Long): UserEntitiesRecord? =
        context.selectFrom(USER_ENTITIES)
            .where(mainCondition.and(USER_ENTITIES.ID.eq(id)))
            .fetchOne()

    private fun toEntity(record: UserEntitiesRecord): T {
        val jsonEntity: String = record.entity
        try {
            val entity = jsonService.parse(jsonEntity, entityClass)
            if (entity.getId() == null) {
                entity.setId(record.id)
            }
            return entity
        } catch (ex: Exception) {
            log.error("Failed to parse entity from json:\n$jsonEntity", ex)
            throw ex
        }
    }

    private fun getOrCreateEntityInfo(): EntityTypesRecord {
        val info = context.selectFrom(ENTITY_TYPES)
            .where(ENTITY_TYPES.TYPE_NAME.eq(entityClass.name))
            .fetchOne()

        if (info != null) {
            return info
        }

        return context.newRecord(ENTITY_TYPES).also { record ->
            record.typeName = entityClass.name
            record.createdAt = TimeUtil.toEpochMillis(LocalDateTime.now())
            record.store()
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(SqliteEntityRepository::class.java)!!
    }
}

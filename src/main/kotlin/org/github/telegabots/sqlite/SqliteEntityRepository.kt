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
import org.github.telegabots.util.runIn
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.concurrent.locks.ReadWriteLock

/**
 * Sqlite implementation of [EntityRepository]
 */
internal class SqliteEntityRepository<T : BaseEntity>(
    private val entityClass: Class<T>,
    private val userId: Long,
    private val context: DSLContext,
    private val jsonService: JsonService,
    readWriteLock: ReadWriteLock
) : EntityRepository<T> {
    private val readLock = readWriteLock.readLock()
    private val writeLock = readWriteLock.writeLock()
    private val entityType = getOrCreateEntityInfo()
    private val mainCondition = USER_ENTITIES.USER_ID.eq(userId).and(USER_ENTITIES.ENTITY_TYPES_ID.eq(entityType.id))

    override fun userId(): Long = userId

    override fun save(entity: T): T {
        val jsonEntity: String = jsonService.toJson(entity)
        writeLock.runIn {
            val record = if (entity.getId() != null) {
                getEntityRecord(entity.getId()!!)
                    ?: error("Entity '${entityClass.name}' with id ${entity.getId()} not found or not belongs to user")
            } else {
                context.newRecord(USER_ENTITIES).also { record ->
                    record.userId = this.userId
                    record.entityTypesId = entityType.id
                }
            }
            record.entity = jsonEntity
            record.updatedAt = TimeUtil.toEpochMillis(LocalDateTime.now())
            record.store()
            entity.setId(record.id)
            return entity
        }
    }

    override fun findById(id: Long): T? {
        return readLock.runIn {
            getEntityRecord(id)
        }?.let { record -> toEntity(record) }
    }

    override fun existsById(id: Long): Boolean {
        readLock.runIn {
            return context.fetchExists(USER_ENTITIES.where(mainCondition.and(USER_ENTITIES.ID.eq(id))))
        }
    }

    override fun delete(id: Long): Boolean {
        writeLock.runIn {
            return context.deleteFrom(USER_ENTITIES)
                .where(mainCondition.and(USER_ENTITIES.ID.eq(id)))
                .execute() > 0
        }
    }

    override fun deleteAll(): Long {
        writeLock.runIn {
            return context.deleteFrom(USER_ENTITIES)
                .where(mainCondition)
                .execute()
                .toLong()
        }
    }

    override fun count(): Long {
        readLock.runIn {
            return context.fetchCount(USER_ENTITIES.where(mainCondition)).toLong()
        }
    }

    override fun findAll(): List<T> {
        return readLock.runIn {
            context.selectFrom(USER_ENTITIES)
                .where(mainCondition)
                .fetch()
        }.map { record -> toEntity(record) }
    }

    override fun findPage(page: Int, size: Int): EntityPage<T> {
        val size = if (size <= 0) EntityRepository.DEFAULT_PAGE_SIZE else size
        val (records, totalElements) = readLock.runIn {
            val offset = page * size
            val list = context.selectFrom(USER_ENTITIES)
                .where(mainCondition)
                .offset(offset.toLong())
                .limit(size)
                .fetch()

            list to count()
        }
        val totalPages = if (totalElements == 0L) 0 else (totalElements - 1) / size + 1

        return EntityPageImpl(
            content = records.map { record -> toEntity(record) },
            page = page,
            size = size,
            totalPages = totalPages.toInt(),
            totalElements = totalElements
        )
    }

    override fun saveAll(entities: List<T>): List<T> {
        writeLock.runIn {
            return entities.map { save(it) }
        }
    }

    fun getContext(): DSLContext = context

    private fun getEntityRecord(id: Long): UserEntitiesRecord? =
        context.selectFrom(USER_ENTITIES)
            .where(mainCondition.and(USER_ENTITIES.ID.eq(id)))
            .fetchOne()

    private fun toEntity(record: UserEntitiesRecord): T {
        val jsonEntity: String = record.entity
        try {
            val entity = jsonService.parse(jsonEntity, entityClass)
            if (entity.getId() == null) {
                // when first time entity is created, it has no id
                entity.setId(record.id)
            }
            return entity
        } catch (ex: Exception) {
            log.error("Failed to parse entity '${entityClass.name}' from json:\n$jsonEntity", ex)
            throw ex
        }
    }

    private fun getOrCreateEntityInfo(): EntityTypesRecord {
        writeLock.runIn {
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
    }

    private companion object {
        val log = LoggerFactory.getLogger(SqliteEntityRepository::class.java)!!
    }
}

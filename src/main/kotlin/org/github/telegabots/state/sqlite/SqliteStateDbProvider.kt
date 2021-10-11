package org.github.telegabots.state.sqlite

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.Location
import org.flywaydb.core.api.configuration.ClassicConfiguration
import org.github.telegabots.api.MessageType
import org.github.telegabots.entity.CommandBlock
import org.github.telegabots.entity.CommandDef
import org.github.telegabots.entity.CommandPage
import org.github.telegabots.entity.StateDef
import org.github.telegabots.jooq.Tables.BLOCKS
import org.github.telegabots.jooq.Tables.GLOBAL_STATES
import org.github.telegabots.jooq.Tables.LOCAL_STATES
import org.github.telegabots.jooq.Tables.PAGES
import org.github.telegabots.jooq.Tables.SHARED_STATES
import org.github.telegabots.jooq.Tables.USER_STATES
import org.github.telegabots.jooq.tables.records.BlocksRecord
import org.github.telegabots.jooq.tables.records.GlobalStatesRecord
import org.github.telegabots.jooq.tables.records.LocalStatesRecord
import org.github.telegabots.jooq.tables.records.PagesRecord
import org.github.telegabots.jooq.tables.records.SharedStatesRecord
import org.github.telegabots.jooq.tables.records.UserStatesRecord
import org.github.telegabots.service.JsonService
import org.github.telegabots.state.StateDbProvider
import org.github.telegabots.util.TimeUtil
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager

/**
 * Sqlite-based implementation of StateDbProvider
 */
class SqliteStateDbProvider(
    private val conn: Connection,
    private val jsonService: JsonService
) : StateDbProvider {
    private val context: DSLContext = DSL.using(conn)

    override fun saveBlock(block: CommandBlock): CommandBlock {
        check(block.isValid()) { "Block is invalid: $block" }

        val blockRecord = findBlockByMessageIdInternal(block.userId, block.messageId) ?: context.newRecord(BLOCKS)

        with(blockRecord) {
            userId = block.userId
            messageId = block.messageId
            messageType = block.messageType.name
            createdAt = TimeUtil.toEpochMillis(block.createdAt)

            store()
        }

        return blockRecord.toDto()
    }

    override fun savePage(page: CommandPage): CommandPage? {
        check(page.isValid()) { "Page is invalid: $page" }

        val pagesRecord = (if (page.id > 0) context.selectFrom(PAGES)
            .where(PAGES.ID.eq(page.id))
            .fetchOne() else null) ?: context.newRecord(PAGES)

        with(pagesRecord) {
            blockId = page.blockId
            createdAt = TimeUtil.toEpochMillis(page.createdAt)
            updatedAt = TimeUtil.toEpochMillis(page.updatedAt)
            commandDefs = toCommandDefsRaw(page.commandDefs)
            handler = page.handler

            store()
        }

        return pagesRecord.toDto()
    }

    override fun findPageById(pageId: Long): CommandPage? =
        context.selectFrom(PAGES)
            .where(PAGES.ID.eq(pageId))
            .fetchOne()
            ?.toDto()

    override fun findBlockById(blockId: Long): CommandBlock? =
        context.selectFrom(BLOCKS)
            .where(BLOCKS.ID.eq(blockId))
            .fetchOne()
            ?.toDto()

    override fun findBlockByMessageId(userId: Long, messageId: Int): CommandBlock? =
        findBlockByMessageIdInternal(userId, messageId)?.toDto()

    override fun findBlockIdByMessageId(userId: Long, messageId: Int): Long? =
        context.select(BLOCKS.ID)
            .where(BLOCKS.USER_ID.eq(userId).and(BLOCKS.MESSAGE_ID.eq(messageId)))
            .fetchOne()
            ?.value1()

    override fun findLastBlockByUserId(userId: Long): CommandBlock? =
        context.selectFrom(BLOCKS)
            .where(BLOCKS.USER_ID.eq(userId))
            .orderBy(BLOCKS.ID.desc())
            .limit(1)
            .fetchOne()
            ?.toDto()

    override fun findLastPageByBlockId(blockId: Long): CommandPage? =
        context.selectFrom(PAGES)
            .where(PAGES.BLOCK_ID.eq(blockId))
            .orderBy(PAGES.ID.desc())
            .limit(1)
            .fetchOne()
            ?.toDto()

    override fun findBlockByPageId(pageId: Long): CommandBlock? =
        context.select(BLOCKS.asterisk())
            .from(BLOCKS.join(PAGES).on(BLOCKS.ID.eq(PAGES.BLOCK_ID)))
            .where(PAGES.ID.eq(pageId))
            .fetchOneInto(BlocksRecord::class.java)
            ?.toDto()

    override fun getBlockPages(blockId: Long): List<CommandPage> =
        context.selectFrom(PAGES)
            .where(PAGES.BLOCK_ID.eq(blockId))
            .fetch()
            .map { it.toDto() }

    override fun getBlocksCount(userId: Long): Int =
        context.fetchCount(
            context.selectFrom(BLOCKS)
                .where(BLOCKS.USER_ID.eq(userId))
        )

    override fun getLastBlocks(userId: Long, lastIndexFrom: Int, pageSize: Int): List<CommandBlock> =
        context.selectFrom(BLOCKS)
            .where(BLOCKS.USER_ID.eq(userId))
            .orderBy(BLOCKS.ID.desc())
            .offset(lastIndexFrom)
            .limit(pageSize)
            .fetch()
            .map { it.toDto() }

    override fun saveLocalState(pageId: Long, state: StateDef) {
        val stateRecord = context.selectFrom(LOCAL_STATES)
            .where(LOCAL_STATES.PAGE_ID.eq(pageId))
            .fetchOne() ?: context.newRecord(LOCAL_STATES)

        stateRecord.stateDef = state.toRaw()
        stateRecord.pageId = pageId

        stateRecord.store()
    }

    override fun findLocalState(pageId: Long): StateDef? =
        context.selectFrom(LOCAL_STATES)
            .where(LOCAL_STATES.PAGE_ID.eq(pageId))
            .fetchOne()
            ?.toDto()

    override fun getLocalStates(blockId: Long): Map<Long, StateDef> =
        context.select(LOCAL_STATES.asterisk())
            .from(
                LOCAL_STATES.join(PAGES).on(LOCAL_STATES.PAGE_ID.eq(PAGES.ID))
                    .join(BLOCKS).on(BLOCKS.ID.eq(PAGES.BLOCK_ID))
            )
            .where(BLOCKS.ID.eq(blockId))
            .fetchInto(LocalStatesRecord::class.java)
            .map { it.pageId to it.toDto() }
            .toMap()

    override fun saveSharedState(userId: Long, messageId: Int, state: StateDef) {
        val stateRecord = findSharedStateInternal(userId, messageId)
            ?: findBlockByMessageIdInternal(userId, messageId)?.let { block ->
                context.newRecord(SHARED_STATES).apply {
                    blockId = block.id
                }
            }

        if (stateRecord != null) {
            stateRecord.stateDef = state.toRaw()
            stateRecord.store()
        } else {
            log.warn("Block not found by userId: {}, messageId: {}", userId, messageId)
        }
    }

    override fun findSharedState(userId: Long, messageId: Int): StateDef? =
        findSharedStateInternal(userId, messageId)?.toDto()

    override fun findUserState(userId: Long): StateDef? =
        context.selectFrom(USER_STATES)
            .where(USER_STATES.USER_ID.eq(userId))
            .fetchOne()
            ?.toDto()

    override fun saveUserState(userId: Long, state: StateDef) {
        val statesRecord = context.selectFrom(USER_STATES)
            .where(USER_STATES.USER_ID.eq(userId))
            .fetchOne() ?: context.newRecord(USER_STATES)

        statesRecord.stateDef = state.toRaw()
        statesRecord.userId = userId

        statesRecord.store()
    }

    override fun findGlobalState(): StateDef? =
        context.selectFrom(GLOBAL_STATES)
            .fetchOne()
            ?.toDto()

    override fun saveGlobalState(state: StateDef) {
        val stateRecord = context.selectFrom(GLOBAL_STATES).fetchOne() ?: context.newRecord(GLOBAL_STATES)

        stateRecord.stateDef = state.toRaw()

        stateRecord.store()
    }

    override fun deleteBlock(blockId: Long): CommandBlock? {
        val oldBlock = findBlockById(blockId)

        context.deleteFrom(BLOCKS)
            .where(BLOCKS.ID.eq(blockId))
            .execute()

        return oldBlock
    }

    override fun deletePage(pageId: Long): CommandPage? {
        val oldPage = findPageById(pageId)

        context.deleteFrom(PAGES)
            .where(PAGES.ID.eq(pageId))
            .execute()

        return oldPage
    }

    fun getAllPages(): List<CommandPage> =
        context.selectFrom(PAGES)
            .fetch()
            .map { it.toDto() }

    fun getAllBlocks(): List<CommandBlock> =
        context.selectFrom(BLOCKS)
            .fetch()
            .map { it.toDto() }

    private fun findSharedStateInternal(userId: Long, messageId: Int): SharedStatesRecord? =
        context.select(SHARED_STATES.asterisk())
            .from(SHARED_STATES.join(BLOCKS).on(SHARED_STATES.BLOCK_ID.eq(BLOCKS.ID)))
            .where(BLOCKS.USER_ID.eq(userId).and(BLOCKS.MESSAGE_ID.eq(messageId)))
            .fetchOneInto(SharedStatesRecord::class.java)

    private fun findBlockByMessageIdInternal(userId: Long, messageId: Int): BlocksRecord? =
        context.selectFrom(BLOCKS)
            .where(BLOCKS.USER_ID.eq(userId).and(BLOCKS.MESSAGE_ID.eq(messageId)))
            .fetchOne()

    private fun PagesRecord.toDto(): CommandPage =
        CommandPage(
            id = this.id,
            blockId = this.blockId,
            handler = this.handler,
            commandDefs = parseCommandDefs(this.commandDefs),
            createdAt = TimeUtil.fromEpochMillis(this.createdAt),
            updatedAt = TimeUtil.fromEpochMillis(this.updatedAt)
        )

    private fun BlocksRecord.toDto(): CommandBlock =
        CommandBlock(
            id = this.id,
            userId = this.userId,
            messageId = this.messageId,
            messageType = MessageType.valueOf(this.messageType),
            createdAt = TimeUtil.fromEpochMillis(this.createdAt)
        )

    private fun LocalStatesRecord.toDto(): StateDef = parseStateDef(this.stateDef)
    private fun SharedStatesRecord.toDto(): StateDef = parseStateDef(this.stateDef)
    private fun UserStatesRecord.toDto(): StateDef = parseStateDef(this.stateDef)
    private fun GlobalStatesRecord.toDto(): StateDef = parseStateDef(this.stateDef)

    private fun parseStateDef(raw: String?): StateDef =
        raw?.let { jsonService.parse(it, StateDef::class.java) } ?: StateDef.Empty

    private fun StateDef.toRaw(): String? = when (this) {
        StateDef.Empty -> null
        else -> jsonService.toJson(this)
    }

    private fun parseCommandDefs(raw: String?): List<List<CommandDef>> =
        raw?.let { jsonService.parse(it, CommandDefsRoot::class.java).defs } ?: emptyList()

    private fun toCommandDefsRaw(defs: List<List<CommandDef>>): String? =
        if (defs.isNotEmpty()) jsonService.toJson(CommandDefsRoot(defs)) else null

    private data class CommandDefsRoot(val defs: List<List<CommandDef>>)

    companion object {
        fun create(dbFilePath: String): SqliteStateDbProvider =
            SqliteStateDbProvider(getConnection(dbFilePath), JsonService())

        private fun getConnection(dbFilePath: String): Connection {
            migrateDb(dbFilePath)

            return DriverManager.getConnection("jdbc:sqlite:$dbFilePath", "", "")
                .apply {
                    this.prepareStatement("PRAGMA foreign_keys = ON;").execute()
                }
        }

        private fun migrateDb(dbFilePath: String) {
            try {
                val config = ClassicConfiguration()
                config.setDataSource("jdbc:sqlite:$dbFilePath", "", "")
                config.setLocations(Location("db/sqlite-migration"))
                val flyway = Flyway(config)
                flyway.migrate()
            } catch (e: Exception) {
                throw IllegalStateException("Migration failed in file $dbFilePath", e)
            }
        }

        private val log = LoggerFactory.getLogger(SqliteStateDbProvider::class.java)
    }
}

package org.github.telegabots.state.sqlite

import org.github.telegabots.api.MessageType
import org.github.telegabots.entity.CommandBlock
import org.github.telegabots.entity.CommandDef
import org.github.telegabots.entity.CommandPage
import org.github.telegabots.entity.StateDef
import org.github.telegabots.jooq.Tables.BLOCKS
import org.github.telegabots.jooq.Tables.LOCAL_STATES
import org.github.telegabots.jooq.Tables.PAGES
import org.github.telegabots.jooq.tables.records.BlocksRecord
import org.github.telegabots.jooq.tables.records.LocalStatesRecord
import org.github.telegabots.jooq.tables.records.PagesRecord
import org.github.telegabots.service.JsonService
import org.github.telegabots.state.StateDbProvider
import org.github.telegabots.util.TimeUtil
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.sql.Connection

/**
 * Sqlite-based implementation of StateDbProvider
 */
class SqliteStateDbProvider(
    private val conn: Connection,
    private val jsonService: JsonService
) : StateDbProvider {
    private val context: DSLContext = DSL.using(conn)


    override fun saveBlock(block: CommandBlock): CommandBlock {
        TODO("Not yet implemented")
    }

    override fun savePage(page: CommandPage): CommandPage? {
        TODO("Not yet implemented")
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

    override fun findBlockByMessageId(userId: Int, messageId: Int): CommandBlock? =
        context.selectFrom(BLOCKS)
            .where(BLOCKS.USER_ID.eq(userId).and(BLOCKS.MESSAGE_ID.eq(messageId)))
            .fetchOne()
            ?.toDto()

    override fun findLastBlockByUserId(userId: Int): CommandBlock? =
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

    override fun getLastBlocks(userId: Int, lastIndexFrom: Int, pageSize: Int): List<CommandBlock> =
        context.selectFrom(BLOCKS)
            .where(BLOCKS.USER_ID.eq(userId))
            .orderBy(BLOCKS.ID.desc())
            .offset(lastIndexFrom)
            .limit(pageSize)
            .fetch()
            .map { it.toDto() }

    override fun saveLocalState(pageId: Long, state: StateDef) {
        TODO("Not yet implemented")
    }

    override fun findLocalState(pageId: Long): StateDef? =
        context.selectFrom(LOCAL_STATES)
            .where(LOCAL_STATES.PAGE_ID.eq(pageId))
            .fetchOne()
            ?.toDto()

    override fun getLocalStates(blockId: Long): Map<Long, StateDef> {
        TODO("Not yet implemented")
    }

    override fun saveSharedState(userId: Int, messageId: Int, state: StateDef) {
        TODO("Not yet implemented")
    }

    override fun findSharedState(userId: Int, messageId: Int): StateDef? {
        TODO("Not yet implemented")
    }

    override fun findUserState(userId: Int): StateDef? {
        TODO("Not yet implemented")
    }

    override fun saveUserState(userId: Int, state: StateDef) {
        TODO("Not yet implemented")
    }

    override fun findGlobalState(): StateDef? {
        TODO("Not yet implemented")
    }

    override fun saveGlobalState(state: StateDef) {
        TODO("Not yet implemented")
    }

    override fun deleteBlock(blockId: Long): CommandBlock? {
        TODO("Not yet implemented")
    }

    override fun deletePage(pageId: Long): CommandPage? {
        TODO("Not yet implemented")
    }

    private fun PagesRecord.toDto(): CommandPage =
        CommandPage(
            id = this.id,
            blockId = this.blockId,
            handler = this.handler,
            commandDefs = parseCommandDefs(this.commandDefs),
            createdAt = TimeUtil.fromEpochToLocal(this.createdAt),
            updatedAt = TimeUtil.fromEpochToLocal(this.updatedAt)
        )

    private fun BlocksRecord.toDto(): CommandBlock =
        CommandBlock(
            id = this.id,
            userId = this.userId,
            messageId = this.messageId,
            messageType = MessageType.valueOf(this.messageType),
            createdAt = TimeUtil.fromEpochToLocal(this.createdAt)
        )

    private fun LocalStatesRecord.toDto(): StateDef = parseStateDef(this.stateDef)

    private fun parseStateDef(raw: String?): StateDef =
        raw?.let { jsonService.parse(it, StateDef::class.java) } ?: StateDef.Empty

    private fun parseCommandDefs(raw: String?): List<List<CommandDef>> =
        raw?.let { jsonService.parse(it, CommandDefsRoot::class.java).defs } ?: emptyList()

    private data class CommandDefsRoot(val defs: List<List<CommandDef>>)
}

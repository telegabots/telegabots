package org.github.telegabots.state.sqlite

import org.github.telegabots.entity.CommandBlock
import org.github.telegabots.entity.CommandPage
import org.github.telegabots.entity.StateDef
import org.github.telegabots.state.StateDbProvider

/**
 * Sqlite-based implementation of StateDbProvider
 */
class SqliteStateDbProvider : StateDbProvider {
    override fun saveBlock(block: CommandBlock): CommandBlock {
        TODO("Not yet implemented")
    }

    override fun savePage(page: CommandPage): CommandPage? {
        TODO("Not yet implemented")
    }

    override fun findPageById(pageId: Long): CommandPage? {
        TODO("Not yet implemented")
    }

    override fun findBlockById(blockId: Long): CommandBlock? {
        TODO("Not yet implemented")
    }

    override fun findBlockByMessageId(userId: Int, messageId: Int): CommandBlock? {
        TODO("Not yet implemented")
    }

    override fun findLastBlockByUserId(userId: Int): CommandBlock? {
        TODO("Not yet implemented")
    }

    override fun findLastPageByBlockId(blockId: Long): CommandPage? {
        TODO("Not yet implemented")
    }

    override fun findBlockByPageId(pageId: Long): CommandBlock? {
        TODO("Not yet implemented")
    }

    override fun getBlockPages(blockId: Long): List<CommandPage> {
        TODO("Not yet implemented")
    }

    override fun getLastBlocks(userId: Int, lastIndexFrom: Int, pageSize: Int): List<CommandBlock> {
        TODO("Not yet implemented")
    }

    override fun saveLocalState(pageId: Long, state: StateDef) {
        TODO("Not yet implemented")
    }

    override fun findLocalState(pageId: Long): StateDef? {
        TODO("Not yet implemented")
    }

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
}

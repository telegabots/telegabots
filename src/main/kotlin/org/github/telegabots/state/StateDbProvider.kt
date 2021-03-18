package org.github.telegabots.state

import org.github.telegabots.entity.CommandPage
import org.github.telegabots.entity.CommandBlock
import org.github.telegabots.entity.StateDef

/**
 * Database state provider
 */
interface StateDbProvider {
    fun saveBlock(block: CommandBlock): CommandBlock

    fun savePage(page: CommandPage): CommandPage

    fun removePage(pageId: Long): CommandPage?

    fun findBlockById(blockId: Long): CommandBlock?

    fun findBlockByMessageId(userId: Int, messageId: Int): CommandBlock?

    fun findLastBlockByUserId(userId: Int): CommandBlock?

    fun findLastPageByBlockId(userId: Int, blockId: Long): CommandPage?

    fun findBlockIdByPageId(userId: Int, pageId: Long): Long?

    fun saveLocalState(pageId: Long, state: StateDef)

    /**
     * Returns local state related specified pageId
     */
    fun getLocalState(pageId: Long): StateDef

    /**
     * Returns all local states related with all pages by specified blockId
     */
    fun getLocalStates(blockId: Long): Map<Long, StateDef>

    fun saveSharedState(userId: Int, messageId: Int, state: StateDef)

    fun getSharedState(userId: Int, messageId: Int): StateDef

    fun getUserState(userId: Int): StateDef

    fun saveUserState(userId: Int, state: StateDef)

    fun getGlobalState(): StateDef

    fun saveGlobalState(state: StateDef)

    fun getBlockPages(blockId: Long): List<CommandPage>
}

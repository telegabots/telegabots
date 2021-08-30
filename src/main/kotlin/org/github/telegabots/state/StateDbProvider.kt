package org.github.telegabots.state

import org.github.telegabots.entity.CommandBlock
import org.github.telegabots.entity.CommandPage
import org.github.telegabots.entity.StateDef
import java.util.concurrent.locks.Lock

/**
 * Database state provider
 */
interface StateDbProvider {
    /**
     * Creates new block
     */
    fun saveBlock(block: CommandBlock): CommandBlock

    /**
     * Create or update page
     */
    fun savePage(page: CommandPage): CommandPage?

    /**
     * Find page by id
     */
    fun findPageById(pageId: Long): CommandPage?

    /**
     * Find block by id
     */
    fun findBlockById(blockId: Long): CommandBlock?

    /**
     * Find block by related message id
     */
    fun findBlockByMessageId(userId: Int, messageId: Int): CommandBlock?

    /**
     * Find last block
     */
    fun findLastBlockByUserId(userId: Int): CommandBlock?

    /**
     * Find last page by block id
     */
    fun findLastPageByBlockId(blockId: Long): CommandPage?

    /**
     * Find block by page id
     */
    fun findBlockByPageId(pageId: Long): CommandBlock?

    /**
     * Returns list of page by block id
     */
    fun getBlockPages(blockId: Long): List<CommandPage>

    /**
     * Returns last blocks from the end
     */
    fun getLastBlocks(userId: Int, lastIndexFrom: Int, pageSize: Int): List<CommandBlock>

    /**
     * Creates or updates local state related with page
     */
    fun saveLocalState(pageId: Long, state: StateDef)

    /**
     * Returns local state related with specified page id
     */
    fun findLocalState(pageId: Long): StateDef?

    /**
     * Returns all local states related with all pages by specified blockId
     */
    fun getLocalStates(blockId: Long): Map<Long, StateDef>

    /**
     * Creates or updates local state related with message
     */
    fun saveSharedState(userId: Int, messageId: Int, state: StateDef)

    /**
     * Returns shared state related with specified block (message)
     */
    fun findSharedState(userId: Int, messageId: Int): StateDef?

    /**
     * Returns user state related with specified user
     */
    fun findUserState(userId: Int): StateDef?

    /**
     * Creates or updates user state
     */
    fun saveUserState(userId: Int, state: StateDef)

    /**
     * Returns global state related with all users
     */
    fun findGlobalState(): StateDef?

    /**
     * Creates or updates global state
     */
    fun saveGlobalState(state: StateDef)

    /**
     * Removes block
     */
    fun deleteBlock(blockId: Long): CommandBlock?

    /**
     * Removes page
     */
    fun deletePage(pageId: Long): CommandPage?
}

fun StateDbProvider.getLocalState(pageId: Long): StateDef = findLocalState(pageId) ?: StateDef.Empty

fun StateDbProvider.getSharedState(userId: Int, messageId: Int): StateDef =
    findSharedState(userId, messageId) ?: StateDef.Empty

fun StateDbProvider.getUserState(userId: Int): StateDef = findUserState(userId) ?: StateDef.Empty

fun StateDbProvider.getGlobalState(): StateDef = findGlobalState() ?: StateDef.Empty

/**
 * Used when several commands need to be atomic
 */
interface LockableStateDbProvider : StateDbProvider {
    fun readLock(): Lock

    fun writeLock(): Lock
}

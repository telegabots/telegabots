package org.github.telegabots.state

import org.github.telegabots.entity.CommandPage
import org.github.telegabots.entity.CommandBlock
import org.github.telegabots.entity.StateDef
import java.util.concurrent.locks.Lock

/**
 * Database state provider
 */
interface StateDbProvider {
    fun saveBlock(block: CommandBlock): CommandBlock

    fun savePage(page: CommandPage): CommandPage

    fun removePage(pageId: Long): CommandPage?

    fun findPageById(pageId: Long): CommandPage?

    fun findBlockById(blockId: Long): CommandBlock?

    fun findBlockByMessageId(userId: Int, messageId: Int): CommandBlock?

    fun findLastBlockByUserId(userId: Int): CommandBlock?

    fun findLastPageByBlockId(blockId: Long): CommandPage?

    fun findBlockByPageId(pageId: Long): CommandBlock?

    fun getBlockPages(blockId: Long): List<CommandPage>

    fun saveLocalState(pageId: Long, state: StateDef)

    /**
     * Returns local state related specified pageId
     */
    fun findLocalState(pageId: Long): StateDef?

    /**
     * Returns all local states related with all pages by specified blockId
     */
    fun getLocalStates(blockId: Long): Map<Long, StateDef>

    fun saveSharedState(userId: Int, messageId: Int, state: StateDef)

    fun findSharedState(userId: Int, messageId: Int): StateDef?

    fun findUserState(userId: Int): StateDef?

    fun saveUserState(userId: Int, state: StateDef)

    fun findGlobalState(): StateDef?

    fun saveGlobalState(state: StateDef)
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

package org.github.telegabots.state

import org.github.telegabots.entity.CommandBlock
import org.github.telegabots.entity.CommandPage
import org.github.telegabots.entity.StateDef
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

/**
 * Not thread-safe implementation of StateDbProvider
 *
 * For thead-safe case use with InternalLockableStateDbProvider
 */
class MemoryStateDbProvider : StateDbProvider {
    private val commandBlocks = mutableListOf<CommandBlock>()
    private val commandPages = mutableMapOf<Long, MutableList<CommandPage>>()
    private val localStates = mutableMapOf<Long, StateDef>()
    private val sharedStates = mutableMapOf<Long, StateDef>()
    private val userStates = mutableMapOf<Int, StateDef>()
    private val pageIds = AtomicLong(1_000)
    private val blockIds = AtomicLong(10_000)
    private var globalState: StateDef? = null

    override fun saveBlock(block: CommandBlock): CommandBlock {
        check(block.isValid()) { "block is invalid: $block" }

        val savedBlock = block.copy(id = blockIds.getAndIncrement())
        commandBlocks.add(savedBlock)

        return savedBlock
    }

    override fun savePage(page: CommandPage): CommandPage {
        check(page.isValid()) { "page is invalid: $page" }

        val block = commandBlocks.find { it.id == page.blockId }
            ?: throw IllegalStateException("Block not found: ${page.blockId}")

        if (page.id > 0) {
            val pages = commandPages.getOrPut(block.id) { mutableListOf() }
            val index = pages.indexOfFirst { it.id == page.id }
            check(index >= 0) { "Page not found by id: ${page.id}, blockId: ${page.blockId}" }
            val oldPage = pages.removeAt(index)
            pages.add(index, page.copy(updatedAt = LocalDateTime.now(), createdAt = oldPage.createdAt))

            return page
        } else {
            val now = LocalDateTime.now()
            val savedPage = page.copy(id = pageIds.getAndIncrement(), createdAt = now, updatedAt = now)
            val pages = commandPages.getOrPut(block.id) { mutableListOf() }
            pages.add(savedPage)

            return savedPage
        }
    }

    override fun removePage(pageId: Long): CommandPage? {
        // TODO: optimize
        commandPages.forEach { (_, pages) ->
            val index = pages.indexOfFirst { it.id == pageId }

            if (index >= 0) {
                return pages.removeAt(index)
            }
        }

        return null
    }

    override fun findPageById(pageId: Long): CommandPage? {
        return commandPages.values.flatten().find { it.id == pageId }
    }

    override fun findBlockByMessageId(userId: Int, messageId: Int): CommandBlock? {
        return commandBlocks.find { it.userId == userId && it.messageId == messageId }
    }

    override fun findLastBlockByUserId(userId: Int): CommandBlock? {
        return commandBlocks.filter { it.userId == userId }.maxByOrNull { it.id }
    }

    override fun findBlockById(blockId: Long): CommandBlock? {
        return commandBlocks.find { it.id == blockId }
    }

    override fun findBlockByPageId(pageId: Long): CommandBlock? {
        // TODO: optimize
        return commandBlocks.find { commandPages[it.id]?.any { p -> p.id == pageId } ?: false }
    }

    override fun findLastPageByBlockId(blockId: Long): CommandPage? {
        return commandPages[blockId]?.lastOrNull()
    }

    override fun saveLocalState(pageId: Long, state: StateDef) {
        localStates[pageId] = state
    }

    override fun findLocalState(pageId: Long): StateDef? {
        return localStates[pageId]
    }

    override fun getLocalStates(blockId: Long): Map<Long, StateDef> {
        return getBlockPages(blockId)
            .filter { localStates[it.id] != null }
            .map { it.id to localStates[it.id]!! }
            .toMap()
    }

    override fun saveSharedState(userId: Int, messageId: Int, state: StateDef) {
        val block = findBlockByMessageId(userId, messageId)
            ?: throw IllegalStateException("Block not found by messageId: $messageId and userId: $userId")

        sharedStates[block.id] = state
    }

    override fun findSharedState(userId: Int, messageId: Int): StateDef? {
        val block = findBlockByMessageId(userId, messageId)

        return if (block != null) sharedStates[block.id] else null
    }

    override fun findUserState(userId: Int): StateDef? {
        return userStates[userId]

    }

    override fun saveUserState(userId: Int, state: StateDef) {
        userStates[userId] = state

    }

    override fun findGlobalState(): StateDef? {
        return globalState
    }

    override fun saveGlobalState(state: StateDef) {
        globalState = state
    }

    override fun deleteBlock(blockId: Long) {
        commandPages.remove(blockId)
        commandBlocks.removeIf { it.id == blockId }
    }

    override fun deletePage(pageId: Long) {
        val block = findBlockByPageId(pageId)

        if (block != null) {
            val pages = commandPages[block.id]

            if (pages != null) {
                pages.removeIf { it.id == pageId }

                if (pages.isEmpty()) {
                    deleteBlock(block.id)
                }
            }
        }
    }

    override fun getBlockPages(blockId: Long): List<CommandPage> {
        return commandPages[blockId] ?: emptyList()
    }

    override fun getLastBlocks(userId: Int, lastIndexFrom: Int, pageSize: Int): List<CommandBlock> {
        return commandBlocks.filter { it.userId == userId }
            .reversed()
            .drop(lastIndexFrom)
            .take(pageSize)
    }

    fun getUserBlocks(userId: Int): List<CommandBlock> {
        return commandBlocks.filter { it.userId == userId }
    }
}

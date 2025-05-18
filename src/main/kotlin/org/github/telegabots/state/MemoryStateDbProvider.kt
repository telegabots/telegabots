package org.github.telegabots.state

import org.github.telegabots.entity.MessageBlock
import org.github.telegabots.entity.MessagePage
import org.github.telegabots.entity.StateDef
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

/**
 * Not thread-safe implementation of StateDbProvider
 *
 * For thead-safe case use with [InternalLockableStateDbProvider]
 */
class MemoryStateDbProvider : StateDbProvider {

    private val messageBlocks = mutableListOf<MessageBlock>()
    private val messagePages = mutableMapOf<Long, MutableList<MessagePage>>()
    private val localStates = mutableMapOf<Long, StateDef>()
    private val sharedStates = mutableMapOf<Long, StateDef>()
    private val userStates = mutableMapOf<Long, StateDef>()
    private val pageIds = AtomicLong(1_000)
    private val blockIds = AtomicLong(10_000)
    private var globalState: StateDef? = null

    override fun saveBlock(block: MessageBlock): MessageBlock {
        check(block.isValid()) { "block is invalid: $block" }

        val savedBlock = block.copy(id = blockIds.getAndIncrement())
        messageBlocks.add(savedBlock)

        return savedBlock
    }

    override fun savePage(page: MessagePage): MessagePage? {
        check(page.isValid()) { "page is invalid: $page" }

        val block = messageBlocks.find { it.id == page.blockId }

        if (block == null) {
            log.warn("Block not found: {} while saving page: {}", page.blockId, page)
            return null
        }

        if (page.id > 0) {
            val pages = messagePages.getOrPut(block.id) { mutableListOf() }
            val index = pages.indexOfFirst { it.id == page.id }
            check(index >= 0) { "Page not found by id: ${page.id}, blockId: ${page.blockId}" }
            val oldPage = pages.removeAt(index)
            pages.add(index, page.copy(updatedAt = LocalDateTime.now(), createdAt = oldPage.createdAt))

            return page
        } else {
            val now = LocalDateTime.now()
            val savedPage = page.copy(id = pageIds.getAndIncrement(), createdAt = now, updatedAt = now)
            val pages = messagePages.getOrPut(block.id) { mutableListOf() }
            pages.add(savedPage)

            return savedPage
        }
    }

    override fun findPageById(pageId: Long): MessagePage? {
        return messagePages.values.flatten().find { it.id == pageId }
    }

    override fun findBlockByMessageId(userId: Long, messageId: Int): MessageBlock? {
        return messageBlocks.find { it.userId == userId && it.messageId == messageId }
    }

    override fun findBlockIdByMessageId(userId: Long, messageId: Int): Long? =
        findBlockByMessageId(userId, messageId)?.id

    override fun findLastBlockByUserId(userId: Long): MessageBlock? {
        return messageBlocks.filter { it.userId == userId }.maxByOrNull { it.id }
    }

    override fun findBlockById(blockId: Long): MessageBlock? {
        return messageBlocks.find { it.id == blockId }
    }

    override fun findBlockByPageId(pageId: Long): MessageBlock? {
        // TODO: optimize
        return messageBlocks.find { messagePages[it.id]?.any { p -> p.id == pageId } ?: false }
    }

    override fun findLastPageByBlockId(blockId: Long): MessagePage? {
        return messagePages[blockId]?.lastOrNull()
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

    override fun saveSharedState(userId: Long, messageId: Int, state: StateDef) {
        val block = findBlockByMessageId(userId, messageId)
            ?: throw IllegalStateException("Block not found by messageId: $messageId and userId: $userId")

        sharedStates[block.id] = state
    }

    override fun findSharedState(userId: Long, messageId: Int): StateDef? {
        val block = findBlockByMessageId(userId, messageId)

        return if (block != null) sharedStates[block.id] else null
    }

    override fun findUserState(userId: Long): StateDef? {
        return userStates[userId]
    }

    override fun saveUserState(userId: Long, state: StateDef) {
        userStates[userId] = state
    }

    override fun findGlobalState(): StateDef? {
        return globalState
    }

    override fun saveGlobalState(state: StateDef) {
        globalState = state
    }

    override fun deleteBlock(blockId: Long): MessageBlock? {
        messagePages.remove(blockId)
        val index = messageBlocks.indexOfFirst { it.id == blockId }

        return if (index >= 0) messageBlocks.removeAt(index) else null
    }

    override fun deletePage(pageId: Long): MessagePage? {
        val block = findBlockByPageId(pageId)

        if (block != null) {
            val pages = messagePages[block.id]

            if (pages != null) {
                val index = pages.indexOfFirst { it.id == pageId }

                val page = if (index >= 0) {
                    pages.removeAt(index)
                } else null

                if (pages.isEmpty()) {
                    deleteBlock(block.id)
                }

                return page
            }
        }

        return null
    }

    override fun getBlockPages(blockId: Long): List<MessagePage> {
        return messagePages[blockId] ?: emptyList()
    }

    override fun getBlocksCount(userId: Long): Int = messageBlocks.size

    override fun getLastBlocks(userId: Long, lastIndexFrom: Int, pageSize: Int): List<MessageBlock> {
        return messageBlocks.filter { it.userId == userId }
            .reversed()
            .drop(lastIndexFrom)
            .take(pageSize)
    }

    fun getUserBlocks(userId: Long): List<MessageBlock> {
        return messageBlocks.filter { it.userId == userId }
    }

    companion object {
        private val log = LoggerFactory.getLogger(MemoryStateDbProvider::class.java)!!
    }
}

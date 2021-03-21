package org.github.telegabots.state

import org.github.telegabots.entity.CommandBlock
import org.github.telegabots.entity.CommandPage
import org.github.telegabots.entity.StateDef
import org.github.telegabots.util.runIn
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

class MemoryStateDbProvider : LockableStateDbProvider {
    private val commandBlocks = mutableListOf<CommandBlock>()
    private val commandPages = mutableMapOf<Long, MutableList<CommandPage>>()
    private val localStates = mutableMapOf<Long, StateDef>()
    private val sharedStates = mutableMapOf<Long, StateDef>()
    private val userStates = mutableMapOf<Int, StateDef>()
    private val pageIds = AtomicLong(1_000)
    private val blockIds = AtomicLong(10_000)
    private var globalState: StateDef? = null
    private val rwl: ReadWriteLock = ReentrantReadWriteLock()
    private val readLock: Lock = rwl.readLock()
    private val writeLock: Lock = rwl.writeLock()

    override fun readLock(): Lock = readLock

    override fun writeLock(): Lock = writeLock

    override fun saveBlock(block: CommandBlock): CommandBlock {
        check(block.isValid()) { "block is invalid: $block" }

        writeLock.runIn {
            val savedBlock = block.copy(id = blockIds.getAndIncrement())
            commandBlocks.add(savedBlock)

            return savedBlock
        }
    }

    override fun savePage(page: CommandPage): CommandPage {
        check(page.isValid()) { "page is invalid: $page" }

        writeLock.runIn {
            val block = commandBlocks.find { it.id == page.blockId }
                ?: throw IllegalStateException("Block not found: ${page.blockId}")

            if (page.id > 0) {
                val pages = commandPages.getOrPut(block.id) { mutableListOf() }
                val index = pages.indexOfFirst { it.id == page.id }
                check(index >= 0) { "Page not found by id: ${page.id}, blockId: ${page.blockId}" }
                pages.removeAt(index)
                pages.add(index, page)

                return page
            } else {
                val savedPage = page.copy(id = pageIds.getAndIncrement())
                val pages = commandPages.getOrPut(block.id) { mutableListOf() }
                pages.add(savedPage)

                return savedPage
            }
        }
    }

    override fun removePage(pageId: Long): CommandPage? {
        writeLock.runIn {
            // TODO: optimize
            commandPages.forEach { (_, pages) ->
                val index = pages.indexOfFirst { it.id == pageId }
                if (index >= 0) {
                    return pages.removeAt(index)
                }
            }

            return null
        }
    }

    override fun findBlockByMessageId(userId: Int, messageId: Int): CommandBlock? {
        readLock.runIn {
            return commandBlocks.find { it.userId == userId && it.messageId == messageId }
        }
    }

    override fun findLastBlockByUserId(userId: Int): CommandBlock? {
        readLock.runIn {
            return commandBlocks.filter { it.userId == userId }.maxByOrNull { it.id }
        }
    }

    override fun findBlockById(blockId: Long): CommandBlock? {
        readLock.runIn {
            return commandBlocks.find { it.id == blockId }
        }
    }

    override fun findLastPageByBlockId(blockId: Long): CommandPage? {
        readLock.runIn {
            return commandPages[blockId]?.lastOrNull()
        }
    }

    override fun findBlockByPageId(pageId: Long): CommandBlock? {
        readLock.runIn {
            // TODO: optimize
            return commandBlocks.find { commandPages[it.id]?.any { p -> p.id == pageId } ?: false }
        }
    }

    override fun saveLocalState(pageId: Long, state: StateDef) {
        writeLock.runIn {
            localStates[pageId] = state
        }
    }

    override fun findLocalState(pageId: Long): StateDef? {
        readLock.runIn {
            return localStates[pageId]
        }
    }

    override fun getLocalStates(blockId: Long): Map<Long, StateDef> {
        readLock.runIn {
            return getBlockPages(blockId)
                .filter { localStates[it.id] != null }
                .map { it.id to localStates[it.id]!! }
                .toMap()
        }
    }

    override fun saveSharedState(userId: Int, messageId: Int, state: StateDef) {
        writeLock.runIn {
            val block = findBlockByMessageId(userId, messageId)
                ?: throw IllegalStateException("Block not found by messageId: $messageId and userId: $userId")

            sharedStates[block.id] = state
        }
    }

    override fun findSharedState(userId: Int, messageId: Int): StateDef? {
        readLock.runIn {
            val block = findBlockByMessageId(userId, messageId)

            return if (block != null) sharedStates[block.id] else null
        }
    }

    override fun findUserState(userId: Int): StateDef? {
        readLock.runIn {
            return userStates[userId]
        }
    }

    override fun saveUserState(userId: Int, state: StateDef) {
        writeLock.runIn {
            userStates[userId] = state
        }
    }

    override fun findGlobalState(): StateDef? {
        readLock.runIn {
            return globalState
        }
    }

    override fun saveGlobalState(state: StateDef) {
        writeLock.runIn {
            globalState = state
        }
    }

    override fun getBlockPages(blockId: Long): List<CommandPage> {
        readLock.runIn {
            return commandPages[blockId] ?: emptyList()
        }
    }

    fun getUserBlocks(userId: Int): List<CommandBlock> {
        readLock.runIn {
            return commandBlocks.filter { it.userId == userId }
        }
    }
}

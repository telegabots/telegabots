package org.github.telegabots.state

import org.github.telegabots.entity.CommandBlock
import org.github.telegabots.entity.CommandPage
import org.github.telegabots.entity.StateDef
import java.util.concurrent.atomic.AtomicLong

class MemoryStateDbProvider : StateDbProvider {
    private val commandBlocks = mutableListOf<CommandBlock>()
    private val commandPages = mutableMapOf<Long, MutableList<CommandPage>>()
    private val localStates = mutableMapOf<Long, StateDef>()
    private val sharedStates = mutableMapOf<Long, StateDef>()
    private val userStates = mutableMapOf<Int, StateDef>()
    private val pageIds = AtomicLong(1_000)
    private val blockIds = AtomicLong(10_000)
    private var globalState: StateDef? = null

    @Synchronized
    override fun saveBlock(block: CommandBlock): CommandBlock {
        check(block.isValid()) { "block is invalid: $block" }

        val savedBlock = block.copy(id = blockIds.getAndIncrement())
        commandBlocks.add(savedBlock)

        return savedBlock
    }

    @Synchronized
    override fun savePage(page: CommandPage): CommandPage {
        check(page.isValid()) { "page is invalid: $page" }

        val block = commandBlocks.find { it.id == page.blockId }
            ?: throw IllegalStateException("Block not found: ${page.blockId}")

        val savedPage = page.copy(id = pageIds.getAndIncrement())
        val pages = commandPages.getOrPut(block.id) { mutableListOf() }
        pages.add(savedPage)

        return savedPage
    }

    override fun removePage(pageId: Long): CommandPage? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @Synchronized
    override fun findBlockByMessageId(userId: Int, messageId: Int): CommandBlock? {
        return commandBlocks.find { it.userId == userId && it.messageId == messageId }
    }

    @Synchronized
    override fun findLastBlockByUserId(userId: Int): CommandBlock? {
        return commandBlocks.filter { it.userId == userId }.maxBy { it.id }
    }

    @Synchronized
    override fun findLastPageByBlockId(userId: Int, blockId: Long): CommandPage? {
        return commandPages[blockId]?.lastOrNull()
    }


    @Synchronized
    override fun saveLocalState(pageId: Long, state: StateDef) {
        localStates[pageId] = state
    }

    @Synchronized
    override fun getLocalState(pageId: Long): StateDef {
        return localStates[pageId] ?: StateDef.Empty
    }

    @Synchronized
    override fun saveSharedState(userId: Int, messageId: Int, state: StateDef) {
        val block = findBlockByMessageId(userId, messageId)
            ?: throw IllegalStateException("Block not found by messageId: $messageId and userId: $userId")

        sharedStates[block.id] = state
    }

    @Synchronized
    override fun getSharedState(userId: Int, messageId: Int): StateDef {
        val block = findBlockByMessageId(userId, messageId)
            ?: throw IllegalStateException("Block not found by messageId: $messageId and userId: $userId")

        return sharedStates[block.id] ?: StateDef.Empty
    }

    @Synchronized
    override fun getUserState(userId: Int): StateDef {
        return userStates[userId] ?: StateDef.Empty
    }

    @Synchronized
    override fun saveUserState(userId: Int, state: StateDef) {
        userStates[userId] = state
    }

    @Synchronized
    override fun getGlobalState(): StateDef {
        return globalState ?: StateDef.Empty
    }

    @Synchronized
    override fun saveGlobalState(state: StateDef) {
        globalState = state
    }

    @Synchronized
    fun getUserBlocks(userId: Int): List<CommandBlock> = commandBlocks.filter { it.userId == userId }

    @Synchronized
    fun getBlockPages(blockId: Long): List<CommandPage> = commandPages[blockId] ?: emptyList()
}

package org.github.telegabots.state

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.LocalizeProvider
import org.github.telegabots.api.MessageType
import org.github.telegabots.api.SubCommand
import org.github.telegabots.entity.CommandBlock
import org.github.telegabots.entity.CommandDef
import org.github.telegabots.entity.CommandPage
import org.github.telegabots.entity.StateDef
import org.github.telegabots.service.JsonService
import org.github.telegabots.util.Validation
import org.github.telegabots.util.runIn
import java.util.concurrent.locks.Lock

/**
 * Stores commands tree and all user-related states
 */
class UserStateService(
    private val userId: Long,
    private val dbProvider: LockableStateDbProvider,
    private val localizeProvider: LocalizeProvider,
    private val jsonService: JsonService,
    private val globalState: StateProvider
) {
    private val sharedStates: MutableMap<Int, StateProvider> = mutableMapOf()
    private val localStates: MutableMap<Long, StateProvider> = mutableMapOf()
    private val userState = UserStateProvider(userId, dbProvider, jsonService)

    fun getReadLock(): Lock = dbProvider.readLock()

    fun getWriteLock(): Lock = dbProvider.writeLock()

    fun getBlockByMessageId(messageId: Int): CommandBlock? = dbProvider.findBlockByMessageId(userId, messageId)

    fun findBlockById(blockId: Long): CommandBlock? = dbProvider.findBlockById(blockId)

    fun getLastBlock(): CommandBlock? = dbProvider.findLastBlockByUserId(userId)

    fun getLastBlocks(lastIndexFrom: Int, pageSize: Int): List<CommandBlock> =
        dbProvider.getLastBlocks(userId, lastIndexFrom, pageSize)

    fun findLastPage(blockId: Long): CommandPage? = dbProvider.findLastPageByBlockId(blockId)

    fun getLastPage(blockId: Long): CommandPage =
        findLastPage(blockId) ?: throw IllegalStateException("Page not found by blockId: $blockId")

    fun getPages(blockId: Long): List<CommandPage> = dbProvider.getBlockPages(blockId)

    fun removePage(pageId: Long): CommandPage? = dbProvider.deletePage(pageId)

    fun findPageById(pageId: Long): CommandPage? = dbProvider.findPageById(pageId)

    fun pageExists(pageId: Long): Boolean = findPageById(pageId) != null

    fun blockExists(blockId: Long): Boolean = findBlockById(blockId) != null

    fun saveBlock(messageId: Int, messageType: MessageType): CommandBlock =
        dbProvider.saveBlock(
            CommandBlock(
                messageId = messageId,
                userId = userId,
                messageType = messageType,
                id = 0
            )
        )

    fun savePage(
        blockId: Long,
        handler: Class<out BaseCommand>,
        subCommands: List<List<SubCommand>> = emptyList(),
        pageId: Long = 0
    ): CommandPage? =
        dbProvider.savePage(
            CommandPage(
                id = pageId,
                blockId = blockId,
                handler = handler.name,
                commandDefs = toCommandDefs(subCommands)
            )
        )

    fun getStates(messageId: Int, pageId: Long): States =
        StatesImpl(
            localState = getLocalStateInternal(pageId),
            sharedState = getSharedState(messageId),
            userState = userState,
            globalState = globalState
        )

    fun getStates(messageId: Int, state: StateDef?, pageId: Long = 0): States =
        StatesImpl(
            localState = if (pageId > 0) getLocalStateInternal(pageId, state) else LocalTempStateProvider(
                state,
                jsonService
            ),
            sharedState = getSharedState(messageId),
            userState = userState,
            globalState = globalState
        )

    fun getStates(): States =
        StatesImpl(
            localState = LocalTempStateProvider(StateDef.Empty, jsonService),
            sharedState = LocalTempStateProvider(StateDef.Empty, jsonService),
            userState = userState,
            globalState = globalState
        )

    fun getLocalStateProvider(pageId: Long): StateProvider {
        return synchronized(localStates) {
            localStates.getOrPut(pageId) { LocalStateProvider(pageId, dbProvider, jsonService) }
        }
    }

    fun getSharedStateProvider(messageId: Int): StateProvider = getSharedState(messageId)

    /**
     * Flushes dirty states to db
     */
    fun flush() {
        TODO()
    }

    /**
     * Clones specified block and returns last page from cloned block
     */
    fun cloneFromBlock(blockId: Long, newMessageId: Int): CommandPage? {
        Validation.validateMessageId(newMessageId)

        dbProvider.writeLock().runIn {
            val block = findBlockById(blockId)

            if (block != null) {
                val pages = getPages(blockId)
                val sharedState = dbProvider.getSharedState(userId, block.messageId)
                val localStates = dbProvider.getLocalStates(blockId)

                val newBlock = dbProvider.saveBlock(
                    CommandBlock(
                        messageId = newMessageId,
                        userId = userId,
                        messageType = block.messageType
                    )
                )

                dbProvider.saveSharedState(userId, newMessageId, sharedState)

                val newPages = pages.map { page ->
                    val newPage = dbProvider.savePage(page.copy(blockId = newBlock.id, id = 0))!!
                    val state = localStates[page.id]

                    if (state != null) {
                        dbProvider.saveLocalState(newPage.id, state)
                    }

                    newPage
                }

                return newPages.last()
            }
        }

        return null
    }

    fun findBlockByPageId(pageId: Long): CommandBlock? = dbProvider.findBlockByPageId(pageId)

    fun findBlockByMessageId(messageId: Int): CommandBlock? = dbProvider.findBlockByMessageId(userId, messageId)

    private fun getSharedState(messageId: Int): StateProvider {
        return synchronized(sharedStates) {
            sharedStates.getOrPut(messageId) { SharedStateProvider(userId, messageId, dbProvider, jsonService) }
        }
    }

    private fun getLocalStateInternal(pageId: Long, state: StateDef? = null): StateProvider =
        if (state != null)
            AdditionalStateProvider(getLocalStateProvider(pageId), state, jsonService)
        else
            getLocalStateProvider(pageId)

    private fun toCommandDefs(subCommands: List<List<SubCommand>>): List<List<CommandDef>> =
        subCommands.map { it.map { cmd -> toCommandDef(cmd) } }

    private fun toCommandDef(cmd: SubCommand): CommandDef =
        CommandDef(
            titleId = cmd.titleId,
            title = cmd.title ?: localizeProvider.getString(cmd.titleId),
            handler = cmd.handler?.name,
            state = jsonService.toStateDef(cmd.state),
            behaviour = cmd.behaviour
        )

    fun deleteBlock(blockId: Long) {
        dbProvider.deleteBlock(blockId)
    }

    fun deletePage(pageId: Long) {
        dbProvider.deletePage(pageId)
    }

    fun mergeLocalStateByPageId(pageId: Long, state: StateDef) {
        getLocalStateProvider(pageId).mergeAll(jsonService.toState(state)!!.items)
    }
}

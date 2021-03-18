package org.github.telegabots.state

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.LocalizeProvider
import org.github.telegabots.api.MessageType
import org.github.telegabots.api.SubCommand
import org.github.telegabots.entity.CommandPage
import org.github.telegabots.entity.CommandBlock
import org.github.telegabots.entity.CommandDef
import org.github.telegabots.entity.StateDef
import org.github.telegabots.service.JsonService
import org.github.telegabots.util.Validation

/**
 * Stores commands tree and all user-related states
 */
class UserStateService(
    private val userId: Int,
    private val dbProvider: StateDbProvider,
    private val localizeProvider: LocalizeProvider,
    private val jsonService: JsonService,
    private val globalState: StateProvider
) {
    private val sharedStates: MutableMap<Int, StateProvider> = mutableMapOf()
    private val localStates: MutableMap<Long, StateProvider> = mutableMapOf()
    private val userState = UserStateProvider(userId, dbProvider, jsonService)

    fun getBlockByMessageId(messageId: Int): CommandBlock? = dbProvider.findBlockByMessageId(userId, messageId)

    fun findBlockById(blockId: Long): CommandBlock? = dbProvider.findBlockById(blockId)

    fun getBlockById(blockId: Long): CommandBlock =
        findBlockById(blockId) ?: throw IllegalStateException("Block by id not found: $blockId")

    fun getLastBlock(): CommandBlock? = dbProvider.findLastBlockByUserId(userId)

    fun findLastPage(blockId: Long): CommandPage? = dbProvider.findLastPageByBlockId(userId, blockId)

    fun getLastPage(blockId: Long): CommandPage =
        findLastPage(blockId) ?: throw IllegalStateException("Page not found by blockId: $blockId")

    fun getPages(blockId: Long): List<CommandPage> = dbProvider.getBlockPages(blockId)

    fun removePage(pageId: Long) = dbProvider.removePage(pageId)

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
        pageId: Long = 0,
        // TODO: redundant field. remove it
        messageId: Int? = null
    ): CommandPage =
        dbProvider.savePage(
            CommandPage(
                id = pageId,
                blockId = blockId,
                handler = handler.name,
                messageId = messageId,
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

    /**
     * Flushes dirty states to db
     */
    fun flush() {
        TODO()
    }

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

    /**
     * Clones specified block and returns last page from cloned block
     */
    fun cloneFromBlock(blockId: Long, newMessageId: Int): CommandPage {
        Validation.validateMessageId(newMessageId)

        val block = getBlockById(blockId)
        val pages = getPages(blockId)
        val sharedState = dbProvider.getSharedState(userId, block.messageId)
        val localStates = dbProvider.getLocalStates(blockId)

        val newBlock = dbProvider.saveBlock(
            CommandBlock(
                messageId = newMessageId,
                userId = userId,
                messageType = block.messageType,
                id = 0
            )
        )

        dbProvider.saveSharedState(userId, newMessageId, sharedState)

        val newPages = pages.map { page ->
            val newPage = dbProvider.savePage(page.copy(blockId = newBlock.id))
            val state = localStates[page.id]

            if (state != null) {
                dbProvider.saveLocalState(newPage.id, state)
            }

            newPage
        }

        return newPages.last()
    }

    fun findBlockIdByPageId(pageId: Long): Long? {
        return dbProvider.findBlockIdByPageId(userId, pageId)
    }
}

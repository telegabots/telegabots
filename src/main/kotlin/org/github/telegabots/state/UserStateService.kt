package org.github.telegabots.state

import org.github.telegabots.BaseCommand
import org.github.telegabots.MessageType
import org.github.telegabots.SubCommand
import org.github.telegabots.entity.CommandPage
import org.github.telegabots.entity.CommandBlock
import org.github.telegabots.entity.CommandDef
import org.github.telegabots.entity.StateDef
import org.github.telegabots.service.JsonService

/**
 * Stores commands tree and all user-related states
 */
class UserStateService(
    private val userId: Int,
    private val dbProvider: StateDbProvider,
    private val jsonService: JsonService,
    private val globalState: StateProvider
) {
    private val sharedStates: MutableMap<Int, StateProvider> = mutableMapOf()
    private val localStates: MutableMap<Long, StateProvider> = mutableMapOf()
    private val userState = UserStateProvider(userId, dbProvider, jsonService)

    fun getBlock(messageId: Int): CommandBlock? = dbProvider.findBlockByMessageId(userId, messageId)

    fun getLastBlock(): CommandBlock? = dbProvider.findLastBlockByUserId(userId)

    fun getLastPage(blockId: Long): CommandPage? = dbProvider.findLastPageByBlockId(userId, blockId)

    fun saveBlock(messageId: Int, messageType: MessageType): CommandBlock =
        dbProvider.saveBlock(
            CommandBlock(
                messageId = messageId,
                userId = userId,
                messageType = messageType,
                id = 0
            )
        )

    fun savePage(blockId: Long, handler: Class<out BaseCommand>, subCommands: List<List<SubCommand>>) =
        dbProvider.savePage(
            CommandPage(
                blockId = blockId,
                handler = handler.name,
                subCommands = toSubCommands(subCommands)
            )
        )

    fun getStates(messageId: Int, pageId: Long): States =
        StatesImpl(
            localState = getLocalState(pageId),
            sharedState = getSharedState(messageId),
            userState = userState,
            globalState = globalState
        )

    fun getStates(messageId: Int, state: StateDef?): States =
        StatesImpl(
            localState = LocalTempStateProvider(state, jsonService),
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

    private fun getLocalState(pageId: Long): StateProvider {
        return synchronized(localStates) {
            localStates.getOrPut(pageId) { LocalStateProvider(pageId, dbProvider, jsonService) }
        }
    }

    private fun toSubCommands(subCommands: List<List<SubCommand>>): List<List<CommandDef>> =
        subCommands.map { it.map { cmd -> toSubcommand(cmd) } }

    private fun toSubcommand(cmd: SubCommand): CommandDef =
        CommandDef(
            titleId = cmd.titleId,
            handler = cmd.handler?.name,
            state = jsonService.toStateDef(cmd.state)
        )
}

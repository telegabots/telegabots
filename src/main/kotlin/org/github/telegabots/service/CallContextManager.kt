package org.github.telegabots.service

import org.github.telegabots.*
import org.github.telegabots.entity.CommandBlock
import org.github.telegabots.entity.CommandDef
import org.github.telegabots.entity.CommandPage
import org.github.telegabots.state.UsersStatesManager
import org.slf4j.LoggerFactory

class CallContextManager(
    private val messageSender: MessageSender,
    private val serviceProvider: ServiceProvider,
    private val commandHandlers: CommandHandlers,
    private val usersStatesManager: UsersStatesManager,
    private val userLocalizationFactory: UserLocalizationFactory,
    private val rootCommand: Class<out BaseCommand>
) {
    private val log = LoggerFactory.getLogger(CallContextManager::class.java)

    fun get(input: InputMessage): CommandCallContext {
        return when (input.type) {
            MessageType.TEXT -> getSimpleMessageContext(input)
            MessageType.CALLBACK -> getCallbackMessageContext(input)
        }
    }

    private fun getSimpleMessageContext(input: InputMessage): CommandCallContext {
        val userState = usersStatesManager.get(input.userId)
        val lastBlock = userState.getLastBlock()

        if (lastBlock != null) {
            val lastPage = userState.getLastPage(lastBlock.id)

            if (lastPage != null) {
                val commandDef = findCommandDef(lastBlock, lastPage, input)

                if (commandDef != null) {
                    if (commandDef.isBackCommand()) {
                        TODO()
                    }

                    if (commandDef.isRefreshCommand()) {
                        TODO()
                    }

                    if (commandDef.handler != null && commandDef.handler.isNotBlank()) {
                        val handler = commandHandlers.getCommandHandler(commandDef.handler)
                        val states = userState.getStates(lastBlock.messageId, commandDef.state)
                        val context = createCommandContext(lastBlock.id, handler.command, input)

                        return CommandCallContext(commandHandler = handler,
                            input = input,
                            states = states,
                            commandContext = context,
                            defaultContext = { getRootCallContext(userState, input) })
                    }
                }

                val handler = commandHandlers.getCommandHandler(lastPage.handler)
                val states = userState.getStates(lastBlock.messageId, lastPage.blockId)
                val context = createCommandContext(lastBlock.id, handler.command, input)

                return CommandCallContext(commandHandler = handler,
                    input = input,
                    states = states,
                    commandContext = context,
                    defaultContext = { getRootCallContext(userState, input) })
            } else {
                log.warn("Last page not found. Input: {}", input)
            }
        }

        return getRootCallContext(userState, input)
    }

    private fun getCallbackMessageContext(input: InputMessage): CommandCallContext {
        val messageId = input.messageId!!
        val userState = usersStatesManager.get(input.userId)
        val lastBlock = userState.getBlock(messageId)

        if (lastBlock != null) {
            val lastPage = userState.getLastPage(lastBlock.id)

            if (lastPage != null) {
                val commandDef = findCommandDef(lastBlock, lastPage, input)

                if (commandDef?.handler != null) {
                    val handler = commandHandlers.getCommandHandler(commandDef.handler)
                    val states = userState.getStates(lastBlock.messageId, commandDef.state)
                    val context = createCommandContext(lastBlock.id, handler.command, input)

                    return CommandCallContext(commandHandler = handler,
                        input = input,
                        states = states,
                        commandContext = context,
                        defaultContext = { getRootCallContext(userState, input) })
                } else {
                    val handler = commandHandlers.getCommandHandler(lastPage.handler)
                    val states = userState.getStates(lastBlock.messageId, lastPage.blockId)
                    val context = createCommandContext(lastBlock.id, handler.command, input)

                    return CommandCallContext(commandHandler = handler,
                        input = input,
                        states = states,
                        commandContext = context,
                        defaultContext = { getRootCallContext(userState, input) })
                }
            } else {
                log.warn("Last command not found. Input: {}", input)
            }
        }


        return getRootCallContext(userState, input)
    }

    private fun getRootCallContext(
        userState: org.github.telegabots.state.UserStateService,
        input: InputMessage
    ): CommandCallContext {
        val handler = commandHandlers.getCommandHandler(rootCommand)
        val states = userState.getStates()
        val context = createCommandContext(blockId = 0, command = handler.command, input = input)

        return CommandCallContext(commandHandler = handler,
            input = input,
            states = states,
            commandContext = context,
            defaultContext = { null })
    }

    private fun findCommandDef(lastBlock: CommandBlock, lastCommand: CommandPage, input: InputMessage): CommandDef? {
        if (lastBlock.messageType == input.type) {
            val localizeProvider = userLocalizationFactory.getProvider(input.userId)

            return when (lastBlock.messageType) {
                MessageType.TEXT -> lastCommand.subCommands.flatten()
                    .find { localizeProvider.getString(it.titleId) == input.query }
                MessageType.CALLBACK -> lastCommand.subCommands.flatten().find { it.titleId == input.query }
            }
        }

        return null
    }

    private fun createCommandContext(blockId: Long, command: BaseCommand, input: InputMessage): CommandContext {
        return CommandContextImpl(
            blockId = blockId,
            command = command,
            input = input,
            commandHandlers = commandHandlers,
            messageSender = messageSender,
            serviceProvider = serviceProvider,
            localizeProvider = userLocalizationFactory.getProvider(input.userId),
            userState = usersStatesManager.get(input.userId)
        )
    }
}

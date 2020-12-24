package org.github.telegabots.service

import org.github.telegabots.*
import org.github.telegabots.entity.CommandBlock
import org.github.telegabots.entity.CommandDef
import org.github.telegabots.entity.CommandPage
import org.github.telegabots.entity.StateDef
import org.github.telegabots.state.UserStateService
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
            MessageType.Text -> getSimpleMessageContext(input)
            MessageType.Callback -> getCallbackMessageContext(input)
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
        val block = userState.getBlock(messageId)

        if (block != null) {
            val lastPage = userState.getLastPage(block.id)

            if (lastPage != null) {
                val commandDef = findCommandDef(block, lastPage, input)

                if (commandDef != null) {
                    val context = createCallContextCommandDef(
                        commandDef,
                        userState,
                        block,
                        lastPage,
                        input
                    )

                    if (context != null) {
                        return context
                    }
                }

                // send input into last page command
                return createCallContext(block, lastPage.handler, userState, input, pageId = lastPage.id)
            } else {
                log.warn("Last command not found. Input: {}", input)
            }
        } else {
            log.warn("Last block not found by messageId: {}. Input: {}", messageId, input)
        }

        return getRootCallContext(userState, input)
    }

    private fun createCallContextCommandDef(
        commandDef: CommandDef,
        userState: UserStateService,
        block: CommandBlock,
        lastPage: CommandPage,
        input: InputMessage
    ): CommandCallContext? {
        if (commandDef.isBackCommand()) {
            val pages = userState.getPages(block.id)
            // remove last page of the block if the page not first page
            if (pages.size > 1) {
                userState.removePage(lastPage.id)
                val prevPage = pages[pages.size - 2]

                return createCallContext(
                    block,
                    prevPage.handler,
                    userState,
                    input.toInputRefresh(),
                    prevPage.id
                )
            } else {
                // if only one page just send refresh command to current command
                return createCallContext(
                    block,
                    lastPage.handler,
                    userState,
                    input.toInputRefresh(),
                    lastPage.id
                )
            }
        }

        if (commandDef.isRefreshCommand()) {
            // send refresh command to current command
            return createCallContext(
                block,
                lastPage.handler,
                userState,
                input.toInputRefresh(),
                lastPage.id
            )
        }

        if (commandDef.handler != null && commandDef.handler.isNotBlank()) {
            // create new page with specified handler
            return createCallContext(
                block,
                commandDef.handler,
                userState,
                input.toInputRefresh(),
                lastPage.id,
                createNewPage = true
            )
        }

        return null
    }

    private fun createCallContext(
        block: CommandBlock,
        handler: String,
        userState: UserStateService,
        input: InputMessage,
        pageId: Long,
        createNewPage: Boolean = false,
        state: StateDef? = null
    ): CommandCallContext {
        val cmdHandler = commandHandlers.getCommandHandler(handler)
        val finalPageId = if (createNewPage) userState.savePage(block.id, cmdHandler.commandClass).id else pageId
        val states = userState.getStates(block.messageId, state, finalPageId)
        val context = createCommandContext(block.id, cmdHandler.command, input, finalPageId)

        return CommandCallContext(commandHandler = cmdHandler,
            input = input,
            states = states,
            commandContext = context,
            defaultContext = { getRootCallContext(userState, input) })
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

    private fun findCommandDef(block: CommandBlock, page: CommandPage, input: InputMessage): CommandDef? {
        if (block.messageType == input.type) {
            val localizeProvider = userLocalizationFactory.getProvider(input.userId)

            return when (block.messageType) {
                MessageType.Text -> page.subCommands.flatten()
                    .find { localizeProvider.getString(it.titleId) == input.query }
                MessageType.Callback -> page.subCommands.flatten().find { it.titleId == input.query }
            } ?: parseSysCommand(block.messageType, input.query)
        }

        return null
    }

    private fun parseSysCommand(messageType: MessageType, query: String): CommandDef? {
        TODO("Not yet implemented")
    }

    private fun createCommandContext(
        blockId: Long,
        command: BaseCommand,
        input: InputMessage,
        pageId: Long = 0L
    ): CommandContext {
        return CommandContextImpl(
            blockId = blockId,
            pageId = pageId,
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

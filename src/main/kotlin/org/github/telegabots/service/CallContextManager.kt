package org.github.telegabots.service

import org.github.telegabots.api.*
import org.github.telegabots.api.entity.CommandBlock
import org.github.telegabots.api.entity.CommandDef
import org.github.telegabots.api.entity.CommandPage
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
            MessageType.Text -> getTextMessageContext(input)
            MessageType.Inline -> getInlineMessageContext(input)
        }
    }

    private fun getTextMessageContext(input: InputMessage): CommandCallContext {
        val userState = usersStatesManager.get(input.userId)
        val lastBlock = userState.getLastBlock()

        return getCommonCallContext(lastBlock, input, userState)
    }

    private fun getInlineMessageContext(input: InputMessage): CommandCallContext {
        val messageId = input.messageId!!
        val userState = usersStatesManager.get(input.userId)
        val block = userState.getBlock(messageId)

        return getCommonCallContext(block, input, userState)
    }

    private fun getCommonCallContext(
        block: CommandBlock?, input: InputMessage, userState: UserStateService
    ): CommandCallContext {
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
            log.warn("Last block not found by input: {}", input)
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
                val prevPage = pages[pages.size - 2]
                userState.removePage(lastPage.id)

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
        userState: UserStateService, input: InputMessage
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
        val commandDef = if (block.messageType == input.type) {
            val localizeProvider = userLocalizationFactory.getProvider(input.userId)

            when (block.messageType) {
                MessageType.Text -> page.subCommands.flatten()
                    .find { localizeProvider.getString(it.titleId) == input.query }
                MessageType.Inline -> page.subCommands.flatten().find { it.titleId == input.query }
            } ?: parseSysCommand(block.messageType, input.query, localizeProvider)
        } else null

        log.debug("Parsed commandDef: {} by input: {}", commandDef, input)

        return commandDef
    }

    private fun parseSysCommand(
        messageType: MessageType,
        query: String,
        localizeProvider: LocalizeProvider
    ): CommandDef? {
        return when (messageType) {
            MessageType.Inline -> when (query) {
                SystemCommands.REFRESH, SystemCommands.GO_BACK -> CommandDef(query, null, null)
                else -> null
            }
            MessageType.Text -> when (query) {
                localizeProvider.getString(SystemCommands.REFRESH) -> CommandDef(SystemCommands.REFRESH, null, null)
                localizeProvider.getString(SystemCommands.GO_BACK) -> CommandDef(SystemCommands.GO_BACK, null, null)
                else -> null
            }
        }
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

package org.github.telegabots.service

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.CommandBehaviour
import org.github.telegabots.api.CommandContext
import org.github.telegabots.api.InputMessage
import org.github.telegabots.api.LocalizeProvider
import org.github.telegabots.api.MessageSender
import org.github.telegabots.api.MessageType
import org.github.telegabots.api.ServiceProvider
import org.github.telegabots.api.SystemCommands
import org.github.telegabots.api.UserLocalizationFactory
import org.github.telegabots.entity.CommandBlock
import org.github.telegabots.entity.CommandDef
import org.github.telegabots.entity.CommandPage
import org.github.telegabots.entity.StateDef
import org.github.telegabots.state.UserStateService
import org.github.telegabots.state.UsersStatesManager
import org.github.telegabots.task.TaskManagerFactory
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
    private val taskManagerFactory = TaskManagerFactory(serviceProvider)

    init {
        val rootHandler = commandHandlers.getCommandHandler(rootCommand)

        check(rootHandler.canHandle(MessageType.Text)) { "Root command (${rootCommand.name}) have to implement text handler. Annotate method with @TextHandler" }
    }

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
        check(input.type == MessageType.Inline) { "Expected Inline, but found ${input.type}" }
        val userState = usersStatesManager.get(input.userId)
        val block = userState.getBlockByMessageId(input.messageId)

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
                    val context = createCallContextByCommandDef(
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
                return createCallContextByBehaviour(
                    block,
                    lastPage.handler,
                    userState,
                    input,
                    pageId = lastPage.id,
                    behaviour = CommandBehaviour.ParentPageState
                )
            } else {
                log.warn("Last command not found. Input: {}", input)
            }
        } else {
            log.warn("Last block not found by input: {}", input)
        }

        return getRootCallContext(userState, input)
    }

    private fun createCallContextByCommandDef(
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

                return createCallContextByBehaviour(
                    block,
                    prevPage.handler,
                    userState,
                    input.toInputRefresh(),
                    prevPage.id,
                    behaviour = CommandBehaviour.ParentPageState
                )
            } else {
                // if only one page just send refresh command to current command
                return createCallContextByBehaviour(
                    block,
                    lastPage.handler,
                    userState,
                    input.toInputRefresh(),
                    lastPage.id,
                    behaviour = CommandBehaviour.ParentPageState
                )
            }
        }

        if (commandDef.isRefreshCommand()) {
            // send refresh command to current command
            return createCallContextByBehaviour(
                block,
                lastPage.handler,
                userState,
                input.toInputRefresh(),
                lastPage.id,
                behaviour = CommandBehaviour.ParentPageState
            )
        }

        if (commandDef.handler != null && commandDef.handler.isNotBlank()) {
            // create new page with specified handler
            return createCallContextByBehaviour(
                block,
                commandDef.handler,
                userState,
                input.toInputRefresh(),
                lastPage.id,
                state = commandDef.state,
                behaviour = commandDef.behaviour ?: CommandBehaviour.ParentPageState
            )
        }

        return createCallContextByBehaviour(
            block,
            lastPage.handler,
            userState,
            input,
            pageId = lastPage.id,
            state = commandDef.state,
            behaviour = CommandBehaviour.ParentPageState
        )
    }

    private fun createCallContextByBehaviour(
        block: CommandBlock,
        handler: String,
        userState: UserStateService,
        input: InputMessage,
        pageId: Long,
        behaviour: CommandBehaviour,
        state: StateDef? = null
    ): CommandCallContext {
        val cmdHandler = commandHandlers.getCommandHandler(handler)

        val (states, context) = when (behaviour) {
            CommandBehaviour.SeparatePage -> {
                val finalPageId = userState.savePage(block.id, cmdHandler.commandClass).id
                if (state != null) {
                    userState.mergeLocalStateByPageId(finalPageId, state)
                }
                val states = userState.getStates(block.messageId, finalPageId)
                val context = createCommandContext(block.id, block.messageId, cmdHandler.command, input, finalPageId)

                states to context
            }
            CommandBehaviour.ParentPage -> {
                val states = userState.getStates(block.messageId, state, pageId = 0)
                val context = createCommandContext(block.id, block.messageId, cmdHandler.command, input, pageId)

                states to context
            }
            CommandBehaviour.ParentPageState -> {
                val states = userState.getStates(block.messageId, state, pageId)
                val context = createCommandContext(block.id, block.messageId, cmdHandler.command, input, pageId)

                states to context
            }
        }

        return CommandCallContext(commandHandler = cmdHandler,
            states = states,
            commandContext = context,
            defaultContext = { getRootCallContext(userState, input) })
    }

    private fun getRootCallContext(
        userState: UserStateService, input: InputMessage
    ): CommandCallContext {
        val handler = commandHandlers.getCommandHandler(rootCommand)
        val states = userState.getStates()
        val context = createCommandContext(
            blockId = 0,
            currentMessageId = input.inlineMessageId ?: 0,
            command = handler.command,
            input = input
        )

        return CommandCallContext(commandHandler = handler,
            states = states,
            commandContext = context,
            defaultContext = { null })
    }

    private fun findCommandDef(block: CommandBlock, page: CommandPage, input: InputMessage): CommandDef? {
        val commandDef = if (block.messageType == input.type) {
            val localizeProvider = userLocalizationFactory.getProvider(input.userId)

            when (block.messageType) {
                MessageType.Text -> page.commandDefs.flatten().find { it.title == input.query }
                MessageType.Inline -> page.commandDefs.flatten().find { it.titleId == input.query }
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
            MessageType.Inline -> SystemCommands.ALL.filter { it == query }
                .map { CommandDef(it, localizeProvider.getString(it), null, null, null) }
                .firstOrNull()
            MessageType.Text -> SystemCommands.ALL.map { it to localizeProvider.getString(it) }
                .filter { it.second == query }
                .map { CommandDef(it.first, it.second, null, null, null) }
                .firstOrNull()
        }
    }

    private fun createCommandContext(
        blockId: Long,
        currentMessageId: Int,
        command: BaseCommand,
        input: InputMessage,
        pageId: Long = 0L
    ): CommandContext {
        return CommandContextImpl(
            blockId = blockId,
            pageId = pageId,
            currentMessageId = currentMessageId,
            command = command,
            input = input,
            commandHandlers = commandHandlers,
            messageSender = messageSender,
            serviceProvider = serviceProvider,
            localizeProvider = userLocalizationFactory.getProvider(input.userId),
            userState = usersStatesManager.get(input.userId),
            taskManagerFactory = taskManagerFactory
        )
    }
}

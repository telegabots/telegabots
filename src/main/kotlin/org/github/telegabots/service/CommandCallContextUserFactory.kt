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
import org.github.telegabots.entity.CommandBlock
import org.github.telegabots.entity.CommandDef
import org.github.telegabots.entity.CommandPage
import org.github.telegabots.entity.StateDef
import org.github.telegabots.state.UserStateService
import org.github.telegabots.task.TaskManagerFactory
import org.github.telegabots.util.runIn
import org.slf4j.LoggerFactory

/**
 * User input related CommandCallContextFactory
 */
class CommandCallContextUserFactory(
    private val input: InputMessage,
    private val messageSender: MessageSender,
    private val serviceProvider: ServiceProvider,
    private val commandHandlers: CommandHandlers,
    private val userState: UserStateService,
    private val localizeProvider: LocalizeProvider,
    private val taskManagerFactory: TaskManagerFactory,
    private val rootCommand: Class<out BaseCommand>
) {
    fun get(): CommandCallContext =
        when (input.type) {
            MessageType.Text -> getTextMessageContext()
            MessageType.Inline -> getInlineMessageContext()
        }

    private fun getTextMessageContext(): CommandCallContext {
        val lastBlock = userState.getLastBlock()

        return getCommonCallContext(lastBlock)
    }

    private fun getInlineMessageContext(): CommandCallContext {
        check(input.type == MessageType.Inline) { "Expected Inline, but found ${input.type}" }

        val block = userState.getBlockByMessageId(input.messageId)

        return getCommonCallContext(block)
    }

    private fun getCommonCallContext(block: CommandBlock?): CommandCallContext {
        if (block != null) {
            userState.getWriteLock().runIn {
                val lastPage = userState.findLastPage(block.id)

                if (lastPage != null) {
                    val commandDef = findCommandDef(block, lastPage)

                    if (commandDef != null) {
                        return createCallContextByCommandDef(
                            commandDef,
                            block,
                            lastPage
                        )
                    }

                    // send input into last page command
                    return createCallContextByBehaviour(
                        block,
                        lastPage.handler,
                        input,
                        pageId = lastPage.id,
                        behaviour = CommandBehaviour.ParentPageState
                    )
                } else {
                    log.warn("Last command not found. Input: {}", input)
                }
            }
        } else {
            log.warn("Block not found by input: {}", input)
        }

        return getRootCallContext()
    }

    private fun createCallContextByCommandDef(
        commandDef: CommandDef,
        block: CommandBlock,
        lastPage: CommandPage
    ): CommandCallContext {
        if (commandDef.isBackCommand()) {
            val pages = userState.getPages(block.id)
            // remove last page of the block if the page not first page
            if (pages.size > 1) {
                val prevPage = pages[pages.size - 2]
                userState.removePage(lastPage.id)

                return createCallContextByBehaviour(
                    block,
                    prevPage.handler,
                    input.toInputRefresh(),
                    prevPage.id,
                    behaviour = CommandBehaviour.ParentPageState
                )
            } else {
                // if only one page just send refresh command to current command
                return createCallContextByBehaviour(
                    block,
                    lastPage.handler,
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
                input.toInputRefresh(),
                lastPage.id,
                behaviour = CommandBehaviour.ParentPageState
            )
        }

        if (commandDef.isNothingCommand()) {
            return NothingCommandCallContext
        }

        if (commandDef.handler != null && commandDef.handler.isNotBlank()) {
            // create new page with specified handler
            return createCallContextByBehaviour(
                block,
                commandDef.handler,
                input.toInputRefresh(),
                lastPage.id,
                state = commandDef.state,
                behaviour = commandDef.behaviour ?: CommandBehaviour.ParentPageState
            )
        }

        return createCallContextByBehaviour(
            block,
            lastPage.handler,
            input,
            pageId = lastPage.id,
            state = commandDef.state,
            behaviour = CommandBehaviour.ParentPageState
        )
    }

    private fun createCallContextByBehaviour(
        block: CommandBlock,
        handler: String,
        input: InputMessage,
        pageId: Long,
        behaviour: CommandBehaviour,
        state: StateDef? = null
    ): CommandCallContext {
        val cmdHandler = commandHandlers.getCommandHandler(handler)

        val (states, context) = when (behaviour) {
            CommandBehaviour.SeparatePage -> {
                val savedPage = userState.savePage(block.id, cmdHandler.commandClass)
                    ?: throw IllegalStateException("Page not created in block: ${block.id}")
                val finalPageId = savedPage.id
                if (state != null) {
                    userState.mergeLocalStateByPageId(finalPageId, state)
                }
                val states = userState.getStates(block.messageId, finalPageId)
                val context =
                    createCommandContext(block.id, block.messageId, cmdHandler.command, input, pageId = finalPageId)

                states to context
            }
            CommandBehaviour.ParentPage -> {
                val states = userState.getStates(block.messageId, state, pageId = 0)
                val context =
                    createCommandContext(block.id, block.messageId, cmdHandler.command, input, pageId = pageId)

                states to context
            }
            CommandBehaviour.ParentPageState -> {
                val states = userState.getStates(block.messageId, state, pageId)
                val context =
                    createCommandContext(block.id, block.messageId, cmdHandler.command, input, pageId = pageId)

                states to context
            }
        }

        return CommandCallContextImpl(commandHandler = cmdHandler,
            states = states,
            commandContext = context,
            defaultContext = { getRootCallContext() })
    }

    private fun getRootCallContext(): CommandCallContext {
        val handler = commandHandlers.getCommandHandler(rootCommand)
        val states = userState.getStates()
        val context = createCommandContext(
            blockId = 0,
            currentMessageId = input.inlineMessageId ?: 0,
            command = handler.command,
            input
        )

        return CommandCallContextImpl(commandHandler = handler,
            states = states,
            commandContext = context,
            defaultContext = { null })
    }

    private fun findCommandDef(block: CommandBlock, page: CommandPage): CommandDef? {
        val commandDef = if (block.messageType == input.type) {
            when (block.messageType) {
                MessageType.Text -> page.commandDefs.flatten().find { it.title == input.query }
                MessageType.Inline -> page.commandDefs.flatten().find { it.titleId == input.query }
            } ?: parseSysCommand(block.messageType, input.query)
        } else null

        log.debug("Parsed commandDef: {}\nby input: {}", commandDef, input)

        return commandDef
    }

    private fun parseSysCommand(
        messageType: MessageType,
        query: String
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
        return BaseContextImpl(
            blockId = blockId,
            pageId = pageId,
            currentMessageId = currentMessageId,
            command = command,
            input = input,
            commandHandlers = commandHandlers,
            messageSender = messageSender,
            serviceProvider = serviceProvider,
            localizeProvider = localizeProvider,
            userState = userState,
            taskManagerFactory = taskManagerFactory
        )
    }

    private companion object {
        val log = LoggerFactory.getLogger(CommandCallContextUserFactory::class.java)!!
    }
}

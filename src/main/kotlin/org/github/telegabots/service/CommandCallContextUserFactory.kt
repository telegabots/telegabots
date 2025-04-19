package org.github.telegabots.service

import org.github.telegabots.api.*
import org.github.telegabots.entity.CommandBlock
import org.github.telegabots.entity.CommandDef
import org.github.telegabots.entity.CommandPage
import org.github.telegabots.entity.StateDef
import org.github.telegabots.state.UserStateService
import org.github.telegabots.task.TaskManagerFactory
import org.github.telegabots.util.runIn
import org.slf4j.LoggerFactory

/**
 * Creates [CommandCallContext] by [InputMessage]
 */
internal class CommandCallContextUserFactory(
    private val input: InputMessage,
    private val messageSender: MessageSender,
    private val serviceProvider: ServiceProvider,
    private val commandHandlers: CommandHandlers,
    private val taskManagerFactory: TaskManagerFactory,
    private val rootCommand: Class<out BaseCommand>
) {
    private val userState: UserStateService =
        serviceProvider.getUserService(UserStateService::class.java, input.userId)
    private val localizationProvider =
        serviceProvider.getUserService(UserLocalizationProvider::class.java, input.userId)

    fun create(): CommandCallContext =
        when (input.type) {
            MessageType.Text -> getTextMessageContext()
            MessageType.Inline -> getInlineMessageContext()
            MessageType.Photo -> error("Input message type not expected: $input")
        }

    private fun getTextMessageContext(): CommandCallContext {
        check(input.type == MessageType.Text) { "Expected Inline, but found ${input.type}" }

        if (BaseCommand.MESSAGE_START == input.query) {
            // always redirect to root command on text input "/start"
            return getRootCallContext()
        }

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
                        // if commandDef is not null, user pressed one of the buttons
                        return createCallContextByCommandDef(
                            commandDef,
                            block,
                            lastPage
                        )
                    }

                    // send input into last page command
                    return createCallContextByPageId(
                        block,
                        lastPage.handler,
                        input,
                        pageId = lastPage.id
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

    /**
     * Creates [CommandCallContext] by [CommandDef]
     */
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
                log.debug("Remove last page: {}. Send refresh command to previous page: {}", lastPage.id, prevPage.id)
                userState.removePage(lastPage.id)

                return createCallContextByPageId(
                    block,
                    prevPage.handler,
                    input.toInputRefresh(),
                    prevPage.id
                )
            } else {
                // if only one page just send refresh command to current command
                log.debug("Only one page in block: {}. Send refresh command", block.id)
                return createCallContextByPageId(
                    block,
                    lastPage.handler,
                    input.toInputRefresh(),
                    lastPage.id
                )
            }
        }

        if (commandDef.isRefreshCommand()) {
            // send refresh command to current command
            return createCallContextByPageId(
                block,
                lastPage.handler,
                input.toInputRefresh(),
                lastPage.id
            )
        }

        if (commandDef.isNothingCommand()) {
            return NothingCommandCallContext
        }

        if (!commandDef.handler.isNullOrBlank()) {
            // create new page with specified handler
            return createCallContextByPageId(
                block,
                commandDef.handler,
                input.toInputRefresh(),
                pageId = null,
                state = commandDef.state
            )
        }

        return createCallContextByPageId(
            block,
            lastPage.handler,
            input,
            pageId = lastPage.id,
            state = commandDef.state
        )
    }

    /**
     * Creates [CommandCallContext] by pageId of specified [CommandBlock]
     *
     * @param block command block
     * @param handler command handler
     * @param input input message
     * @param pageId target page id, if null - create new page
     * @param state additional local state
     */
    private fun createCallContextByPageId(
        block: CommandBlock,
        handler: String,
        input: InputMessage,
        pageId: Long?,
        state: StateDef? = null
    ): CommandCallContext {
        val cmdHandler = commandHandlers.getCommandHandler(handler)
        val pageId: Long = if (pageId == null) {
            // create new page
            val savedPage = userState.savePage(block.id, cmdHandler.commandClass)
                ?: error("Page not created in block: ${block.id}")
            savedPage.id
        } else
            pageId

        val states = userState.getStates(block.messageId, state, pageId)
        val context = createCommandContext(
            block.id,
            block.messageId,
            block.messageType,
            cmdHandler.command,
            input,
            pageId = pageId
        )

        return CommandCallContextImpl(
            commandHandler = cmdHandler,
            states = states,
            commandContext = context,
            defaultContext = { getRootCallContext() })
    }

    /**
     * Creates [CommandCallContext] for root command
     */
    private fun getRootCallContext(): CommandCallContext {
        val handler = commandHandlers.getCommandHandler(rootCommand)
        val states = userState.getStates()
        val context = createCommandContext(
            blockId = 0,
            currentMessageId = input.inlineMessageId ?: 0,
            messageType = MessageType.Text,
            command = handler.command,
            input
        )

        return CommandCallContextImpl(
            commandHandler = handler,
            states = states,
            commandContext = context,
            defaultContext = { null })
    }

    private fun findCommandDef(block: CommandBlock, page: CommandPage): CommandDef? {
        val commandDef = if (canHandleInput(block.messageType)) {
            when (block.messageType) {
                MessageType.Text -> page.commandDefs.flatten().find { it.title == input.query }
                MessageType.Inline, MessageType.Photo -> page.commandDefs.flatten().find { it.titleId == input.query }
            } ?: parseSysCommand(block.messageType, input.query)
        } else null

        log.debug("Parsed commandDef: {}\nby input: {}", commandDef, input)

        return commandDef
    }

    private fun canHandleInput(blockMessageType: MessageType): Boolean {
        return when (blockMessageType) {
            MessageType.Text -> input.type == MessageType.Text
            MessageType.Inline -> input.type == MessageType.Inline
            MessageType.Photo -> input.type == MessageType.Inline
        }
    }

    /**
     * Parse [CommandDef] from [SystemCommands]
     */
    private fun parseSysCommand(
        messageType: MessageType,
        query: String
    ): CommandDef? {
        return when (messageType) {
            MessageType.Inline, MessageType.Photo -> SystemCommands.ALL.filter { it == query }
                .map { CommandDef(it, localizationProvider.getString(it), null, null) }
                .firstOrNull()

            MessageType.Text -> SystemCommands.ALL.map { it to localizationProvider.getString(it) }
                .filter { it.second == query }
                .map { CommandDef(it.first, it.second, null, null) }
                .firstOrNull()
        }
    }

    private fun createCommandContext(
        blockId: Long,
        currentMessageId: Int,
        messageType: MessageType,
        command: BaseCommand,
        input: InputMessage,
        pageId: Long = 0L
    ): CommandContext {
        return BaseContextImpl(
            blockId = blockId,
            pageId = pageId,
            currentMessageId = currentMessageId,
            messageType = messageType,
            command = command,
            input = input,
            commandHandlers = commandHandlers,
            messageSender = messageSender,
            serviceProvider = serviceProvider,
            userState = userState,
            taskManagerFactory = taskManagerFactory
        )
    }

    private companion object {
        val log = LoggerFactory.getLogger(CommandCallContextUserFactory::class.java)!!
    }
}

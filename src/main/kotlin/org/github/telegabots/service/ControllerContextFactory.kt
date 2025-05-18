package org.github.telegabots.service

import org.github.telegabots.api.*
import org.github.telegabots.entity.MessageBlock
import org.github.telegabots.entity.ButtonDef
import org.github.telegabots.entity.MessagePage
import org.github.telegabots.entity.StateDef
import org.github.telegabots.state.States
import org.github.telegabots.state.UserStateService
import org.github.telegabots.task.TaskManagerFactory
import org.github.telegabots.util.runIn
import org.slf4j.LoggerFactory

/**
 * Creates [ControllerContext] by [InputMessage]
 */
internal class ControllerContextFactory(
    private val input: InputMessage,
    private val messageSender: MessageSender,
    private val serviceProvider: ServiceProvider,
    private val controllerHandlers: ControllerHandlers,
    private val taskManagerFactory: TaskManagerFactory,
    private val rootController: Class<out BaseController>
) {
    private val userState: UserStateService =
        serviceProvider.getUserService(UserStateService::class.java, input.userId)
    private val localizationProvider =
        serviceProvider.getUserService(UserLocalizationProvider::class.java, input.userId)

    fun create(): ControllerContext =
        when (input.type) {
            MessageType.Text -> getTextMessageContext()
            MessageType.Inline -> getInlineMessageContext()
            MessageType.Photo -> error("Input message type not expected: $input")
        }

    private fun getTextMessageContext(): ControllerContext {
        check(input.type == MessageType.Text) { "Expected Inline, but found ${input.type}" }

        if (BaseController.MESSAGE_START == input.query) {
            // always redirect to root controller on text input "/start"
            return getRootCallContext()
        }

        val lastBlock = userState.getLastBlock()

        return getCommonCallContext(lastBlock)
    }

    private fun getInlineMessageContext(): ControllerContext {
        check(input.type == MessageType.Inline) { "Expected Inline, but found ${input.type}" }

        val block = userState.getBlockByMessageId(input.messageId)

        return getCommonCallContext(block)
    }

    private fun getCommonCallContext(block: MessageBlock?): ControllerContext {
        if (block != null) {
            userState.getWriteLock().runIn {
                val lastPage = userState.findLastPage(block.id)

                if (lastPage != null) {
                    val buttonDef = findButtonDef(block, lastPage)

                    if (buttonDef != null) {
                        // if buttonDef is not null, user pressed one of the buttons
                        return createCallContextByButtonDef(
                            buttonDef,
                            block,
                            lastPage
                        )
                    }

                    // send input into last page controller
                    return createCallContextByPageId(
                        block,
                        lastPage.handler,
                        input,
                        pageId = lastPage.id
                    )
                } else {
                    log.warn("Last controller not found. Input: {}", input)
                }
            }
        } else {
            log.warn("Block not found by input: {}", input)
        }

        if (input.type == MessageType.Inline) {
            log.warn("Block or page not found. Ignore input: {}", input)
            return NothingControllerCallContext
        }

        return getRootCallContext()
    }

    /**
     * Creates [ControllerContext] by [ButtonDef]
     */
    private fun createCallContextByButtonDef(
        buttonDef: ButtonDef,
        block: MessageBlock,
        lastPage: MessagePage
    ): ControllerContext {
        if (buttonDef.isBackMessage()) {
            val pages = userState.getPages(block.id)
            // remove last page of the block if the page not first page
            if (pages.size > 1) {
                val prevPage = pages[pages.size - 2]
                log.debug("Remove last page: {}. Send refresh controller to previous page: {}", lastPage.id, prevPage.id)
                userState.removePage(lastPage.id)

                return createCallContextByPageId(
                    block,
                    prevPage.handler,
                    input.toInputRefresh(),
                    prevPage.id
                )
            } else {
                // if only one page just send a refresh message to the current controller
                log.debug("Only one page in block: {}. Send refresh message", block.id)
                return createCallContextByPageId(
                    block,
                    lastPage.handler,
                    input.toInputRefresh(),
                    lastPage.id
                )
            }
        }

        if (buttonDef.isRefreshMessage()) {
            // send a refresh message to current controller
            return createCallContextByPageId(
                block,
                lastPage.handler,
                input.toInputRefresh(),
                lastPage.id
            )
        }

        if (buttonDef.isNothingMessage()) {
            return NothingControllerCallContext
        }

        if (!buttonDef.handler.isNullOrBlank()) {
            // create new page with specified handler
            return createCallContextByPageId(
                block,
                buttonDef.handler,
                input.toInputRefresh(),
                // page will be created on update/create
                pageId = 0,
                state = buttonDef.state
            )
        }

        return createCallContextByPageId(
            block,
            lastPage.handler,
            input,
            pageId = lastPage.id,
            state = buttonDef.state
        )
    }

    /**
     * Creates [ControllerContext] by pageId of specified [MessageBlock]
     *
     * @param block message block
     * @param handler controller handler
     * @param input input message
     * @param pageId target page id, if 0 - new page will be created on update/create
     * @param state additional local state
     */
    private fun createCallContextByPageId(
        block: MessageBlock,
        handler: String,
        input: InputMessage,
        pageId: Long,
        state: StateDef? = null
    ): ControllerContext {
        val cmdHandler = controllerHandlers.getControllerHandler(handler)
        val states = userState.getStates(block.messageId, state, pageId)
        return createControllerContext(
            block.id,
            block.messageId,
            block.messageType,
            controllerHandler = cmdHandler,
            states = states,
            input = input,
            pageId = pageId
        )
    }

    /**
     * Creates [ControllerContext] for root controller
     */
    private fun getRootCallContext(): ControllerContext {
        val handler = controllerHandlers.getControllerHandler(rootController)
        val states = userState.getStates()
        return createControllerContext(
            blockId = 0,
            currentMessageId = input.inlineMessageId ?: 0,
            messageType = MessageType.Text,
            controllerHandler = handler,
            states = states,
            input = input
        )
    }

    private fun findButtonDef(block: MessageBlock, page: MessagePage): ButtonDef? {
        val buttonDef = if (canHandleInput(block.messageType)) {
            when (block.messageType) {
                MessageType.Text -> page.buttonDefs.flatten().find { it.title == input.query }
                MessageType.Inline, MessageType.Photo -> page.buttonDefs.flatten().find { it.titleId == input.query }
            } ?: parseSysMessage(block.messageType, input.query)
        } else null

        log.debug("Parsed buttonDef: {}\nby input: {}", buttonDef, input)

        return buttonDef
    }

    private fun canHandleInput(blockMessageType: MessageType): Boolean {
        return when (blockMessageType) {
            MessageType.Text -> input.type == MessageType.Text
            MessageType.Inline -> input.type == MessageType.Inline
            MessageType.Photo -> input.type == MessageType.Inline
        }
    }

    /**
     * Parse [ButtonDef] from [SystemMessages]
     */
    private fun parseSysMessage(
        messageType: MessageType,
        query: String
    ): ButtonDef? {
        return when (messageType) {
            MessageType.Inline, MessageType.Photo -> SystemMessages.ALL.filter { it == query }
                .map { ButtonDef(it, localizationProvider.getString(it), null, null) }
                .firstOrNull()

            MessageType.Text -> SystemMessages.ALL.map { it to localizationProvider.getString(it) }
                .filter { it.second == query }
                .map { ButtonDef(it.first, it.second, null, null) }
                .firstOrNull()
        }
    }

    private fun createControllerContext(
        blockId: Long,
        currentMessageId: Int,
        messageType: MessageType,
        input: InputMessage,
        controllerHandler: ControllerHandler,
        states: States,
        pageId: Long = 0L
    ): ControllerContext {
        return BaseContextImpl(
            blockId = blockId,
            pageId = pageId,
            currentMessageId = currentMessageId,
            messageType = messageType,
            input = input,
            controllerHandler = controllerHandler,
            states = states,
            controllerHandlers = controllerHandlers,
            messageSender = messageSender,
            serviceProvider = serviceProvider,
            userState = userState,
            taskManagerFactory = taskManagerFactory,
            rootController = rootController
        )
    }

    private companion object {
        val log = LoggerFactory.getLogger(ControllerContextFactory::class.java)!!
    }
}

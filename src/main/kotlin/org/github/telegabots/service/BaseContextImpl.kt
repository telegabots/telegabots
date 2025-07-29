package org.github.telegabots.service

import org.github.telegabots.MessageFile
import org.github.telegabots.api.*
import org.github.telegabots.entity.MessageBlock
import org.github.telegabots.entity.MessagePage
import org.github.telegabots.state.StateKind
import org.github.telegabots.state.States
import org.github.telegabots.state.UserStateService
import org.github.telegabots.task.TaskManagerFactory
import org.github.telegabots.util.runIn
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import java.util.function.Consumer

/**
 * Implementation of [ControllerContext] and [TaskContext]
 */
internal class BaseContextImpl(
    private val blockId: Long,
    private val pageId: Long,
    private val messageType: MessageType,
    /**
     * Message id related with current block
     */
    private val currentMessageId: Int,
    private val input: InputMessage,
    private val controllerHandlers: ControllerHandlers,
    private val controllerHandler: ControllerHandler,
    private val states: States,
    private val userState: UserStateService,
    private val serviceProvider: ServiceProvider,
    private val messageSender: MessageSender,
    private val taskManagerFactory: TaskManagerFactory,
    private val rootController: Class<out BaseController>
) : ControllerContext, TaskContext {
    private val jsonService = serviceProvider.getService(InternalJsonService::class.java)
    private val localizationProvider =
        serviceProvider.getUserService(UserLocalizationProvider::class.java, userState.userId())
    private val taskManager = lazy { taskManagerFactory.create(this) }

    override fun inputMessage(): InputMessage = input

    override fun messageId(): Int = currentMessageId

    override fun inputMessageId(): Int = input.messageId

    override fun blockId(): Long = blockId

    override fun pageId(): Long = pageId

    override fun messageType(): MessageType = messageType

    override fun execute(): Boolean {
        if (!controllerHandler.canHandle(input.type)) {
            if (input.type == MessageType.Text) {
                val success = getRootCallContext().execute()
                if (success) {
                    return success
                }
            }

            log.error("Controller handler not found for message: $input")
            error("Message of type ${input.type} can not be handled by controller: ${controllerHandler.controller.javaClass.name}")
        }

        logContext()

        val success = when (input.type) {
            MessageType.Text -> controllerHandler.executeText(input.query, states, this)
            MessageType.Inline -> {
                controllerHandler.executeInline(input.query, states, this)
                true
            }

            MessageType.Photo -> error("Input message type not expected: $input")
        }

        tryGetService(ControllerInterceptor::class.java)?.let { controllerInterceptor ->
            try {
                controllerInterceptor.executed(controllerHandler.controller, input.type, success)
            } catch (ex: Exception) {
                log.error(
                    "Interceptor call failed on controller {} with error: {}",
                    controllerHandler.controller.javaClass.simpleName,
                    ex.message,
                    ex
                )
            }
        }

        states.flush()

        if (!success) {
            log.error("Failed to execute controller: ${controllerHandler.controller.javaClass.simpleName} for input: $input")
            return false
        }

        return true
    }

    override fun create(clazz: Class<out BaseController>, messageType: MessageType?, message: String?): Boolean {
        val handler = controllerHandlers.getControllerHandler(clazz)
        val messageType = getFinalMessageType(messageType, handler)
        val states = userState.getStates()
        val context = createControllerContext(
            blockId = 0,
            currentMessageId = 0,
            messageType = messageType,
            controllerHandler = handler,
            states = states,
            input = input.copy(type = messageType, inlineMessageId = null, messageId = 0, query = message ?: SystemMessages.REFRESH)
        )
        return context.execute()
    }

    override fun currentController(): BaseController = controllerHandler.controller

    override fun createPage(page: Page): Long {
        validatePage(page)

        val messageId = when (page.messageType) {
            MessageType.Text, MessageType.Inline -> messageSender.sendMessage(
                chatId = input.chatId.toString(),
                contentType = page.contentType,
                disablePreview = page.disablePreview,
                message = page.message,
                preSendHandler = { msg ->
                    applyMessageButtons(msg, page.buttons, page.messageType)
                })

            MessageType.Photo -> messageSender.sendImage(
                chatId = input.chatId.toString(),
                file = page.file ?: error("MessageFile is required for photo message"),
                caption = page.message,
                captionContentType = page.contentType,
                disableNotification = page.disableNotification,
                preSendHandler = { msg ->
                    applyMessageButtons(msg, page.buttons)
                })
        }

        userState.getWriteLock().runIn {
            // create new block
            val block = userState.saveBlock(
                messageId = messageId,
                messageType = page.messageType
            )

            val savedPage = userState.savePage(
                block.id,
                handler = page.handler ?: controllerHandler.controllerClass,
                buttons = page.buttons
            )!!

            if (page.state != null) {
                saveLocalState(savedPage.id, page.state.items)
            }

            if (log.isDebugEnabled) {
                log.debug(
                    "Create page. blockId: {}, pageId: {}, input: {},\npage: {}",
                    block.id,
                    savedPage.id,
                    input,
                    jsonService.toPrettyJson(page)
                )
            }

            return savedPage.id
        }
    }

    override fun addPage(page: Page): Long? {
        validatePage(page)

        if (page.blockId > 0) {
            return addPageExplicit(page, page.blockId)
        }

        if (blockId <= 0) {
            return createPage(page)
        }

        return addPageExplicit(page, blockId)
    }

    override fun updatePage(page: Page): Long? {
        validatePage(page)

        if (page.blockId > 0) {
            return updatePageExplicit(page, page.blockId, pageId = page.id)
        }

        if (page.id > 0) {
            val blockByPageId = findBlockIdByPageId(page.id)

            if (blockByPageId != null) {
                return updatePageExplicit(page, blockByPageId, pageId = page.id)
            }

            return null
        }

        if (blockId <= 0 || messageType != page.messageType) {
            // create page if blockId is not specified
            // or message type is not compatible type
            return createPage(page)
        }

        val finalPageId = if (pageId <= 0) {
            // create page if pageId is not specified
            val savedPage = userState.savePage(blockId, controllerHandler.controllerClass)
                ?: error("Page not created in block: $blockId")
            savedPage.id
        } else
            pageId

        return updatePageExplicit(page, blockId, pageId = finalPageId)
    }

    override fun refreshPage(pageId: Long, state: StateRef?): Long? {
        val finalPageId = if (pageId > 0) pageId else pageId()

        val callContext = userState.getWriteLock().runIn {
            val page = userState.findPageById(finalPageId)

            if (page == null) {
                log.warn("Page not found by id: {} while refresh", finalPageId)
                return null
            }

            val block = userState.findBlockById(page.blockId)!!

            if (block.messageType != MessageType.Inline) {
                log.warn(
                    "Page (id={}) can not be refreshed. Required Inline page, but found {}",
                    finalPageId,
                    block.messageType
                )
                return null
            }

            val handler = controllerHandlers.getControllerHandler(page.handler)
            // TODO: improve userState to not use jsonService.toStateDef
            val states = userState.getStates(
                messageId = block.messageId,
                pageId = finalPageId,
                state = jsonService.toStateDef(state)
            )
            val newInput = input.copy(
                type = MessageType.Inline,
                query = SystemMessages.REFRESH,
                messageId = block.messageId,
                inlineMessageId = block.messageId
            )
            createControllerContext(
                blockId = block.id,
                pageId = finalPageId,
                messageType = block.messageType,
                currentMessageId = block.messageId,
                controllerHandler = handler,
                states = states,
                input = newInput
            )
        }

        callContext.execute()

        return finalPageId
    }

    override fun deletePage(pageId: Long): Long? {
        val finalPageId = if (pageId > 0) pageId else pageId()

        userState.getWriteLock().runIn {
            val block = userState.findBlockByPageId(finalPageId)

            if (block == null) {
                log.warn("Page not found by id: {} while deleting page", finalPageId)
                return null
            }

            val pages = userState.getPages(block.id)

            if (pages.size > 1) {
                userState.deletePage(block.id)

                if (block.messageType == MessageType.Inline) {
                    // refresh current last page
                    val lastPage = pages.last { it.id != finalPageId }
                    refreshPage(lastPage.id)
                }
            } else {
                userState.deleteBlock(block.id)
                messageSender.deleteMessage(input.chatId.toString(), block.messageId)
            }

            return finalPageId
        }
    }

    override fun deleteBlock(blockId: Long): Long? {
        val finalBlockId = if (blockId > 0) blockId else blockId()

        userState.getWriteLock().runIn {
            val block = userState.findBlockById(finalBlockId)

            if (block == null) {
                log.warn("Block not found by id: {} while deleting block", finalBlockId)
                return null
            }

            userState.deleteBlock(block.id)
            messageSender.deleteMessage(input.chatId.toString(), block.messageId)

            return finalBlockId
        }
    }

    override fun deleteMessage(messageId: Int) {
        val block = userState.findBlockByMessageId(messageId)

        if (block != null) {
            userState.deleteBlock(block.id)
        }

        messageSender.deleteMessage(input.chatId.toString(), messageId)
    }

    override fun pageVisible(pageId: Long): Boolean {
        val finalPageId = if (pageId > 0) pageId else pageId()

        if (finalPageId <= 0) {
            return false
        }

        val block = userState.findBlockByPageId(finalPageId)
        val lastPage = block?.id?.let { userState.findLastPage(it) }

        return lastPage?.id ?: 0L == finalPageId
    }

    override fun pageExists(pageId: Long): Boolean {
        val finalPageId = if (pageId > 0) pageId else pageId()
        return userState.pageExists(finalPageId)
    }

    override fun blockExists(blockId: Long): Boolean {
        val finalBlockId = if (blockId > 0) blockId else blockId()
        return userState.blockExists(finalBlockId)
    }

    override fun getLastBlocks(lastIndexFrom: Int): List<BlockInfo> {
        val pages = userState.getLastBlocks(lastIndexFrom, 10)

        return pages.map { mapBlockInfo(it) }
    }

    override fun getLastBlock(): BlockInfo? = userState.getLastBlock()?.let { mapBlockInfo(it) }

    override fun getBlockPages(blockId: Long): List<PageInfo> {
        val finalBlockId = if (blockId > 0) blockId else blockId()
        return userState.getPages(finalBlockId).map { mapPageInfo(it) }
    }

    override fun getPageState(pageId: Long): PageStateInfo {
        val finalPageId = if (pageId > 0) pageId else pageId()

        if (userState.pageExists(finalPageId)) {
            val provider = userState.getLocalStateProvider(finalPageId)
            return PageStateInfo(finalPageId, states = provider.getAll())
        }

        return PageStateInfo(finalPageId, emptyList())
    }

    override fun getBlockState(blockId: Long): BlockStateInfo {
        val finalBlockId = if (blockId > 0) blockId else blockId()
        val block = userState.findBlockById(finalBlockId)

        if (block != null) {
            val provider = userState.getSharedStateProvider(block.messageId)
            return BlockStateInfo(pageId, states = provider.getAll())
        }

        return BlockStateInfo(pageId, emptyList())
    }

    private fun mapBlockInfo(it: MessageBlock) = BlockInfo(it.id, createdAt = it.createdAt)

    private fun mapPageInfo(it: MessagePage): PageInfo =
        PageInfo(it.id, createdAt = it.createdAt, updatedAt = it.updatedAt)

    /**
     * Adds page to specified block
     */
    private fun addPageExplicit(
        page: Page,
        blockId: Long,
    ): Long? {
        userState.getWriteLock().runIn {
            val block = userState.findBlockById(blockId)

            if (block == null) {
                log.warn("Block not found by id: {} while adding page: {}", blockId, page)
                return null
            }

            check(page.messageType == block.messageType) { "Adding page message type mismatch block's type. Expected: ${block.messageType}" }

            val finalButtons = addBackButtonIf(page, blockId, false)
            when (page.messageType) {
                MessageType.Text -> {
                    messageSender.sendMessage(
                        chatId = input.chatId.toString(),
                        contentType = page.contentType,
                        disablePreview = page.disablePreview,
                        message = page.message,
                        preSendHandler = Consumer { msg ->
                            applyMessageButtons(msg, finalButtons, page.messageType)
                        })
                }

                MessageType.Inline -> {
                    messageSender.updateMessage(
                        chatId = input.chatId.toString(),
                        messageId = block.messageId,
                        contentType = page.contentType,
                        disablePreview = page.disablePreview,
                        message = page.message,
                        preSendHandler = Consumer { msg ->
                            applyMessageButtons(msg, finalButtons)
                        })
                }

                MessageType.Photo -> {
                    messageSender.updateImage(
                        chatId = input.chatId.toString(),
                        messageId = block.messageId,
                        caption = page.message,
                        captionContentType = page.contentType,
                        file = page.file ?: error("MessageFile is required for photo message"),
                        preSendHandler = Consumer { msg ->
                            applyMessageButtons(msg, finalButtons)
                        })
                }
            }

            val savedPage = userState.savePage(
                blockId,
                handler = page.handler ?: controllerHandler.controllerClass,
                buttons = page.buttons
            )!!

            if (page.state != null) {
                saveLocalState(savedPage.id, page.state.items)
            }

            if (log.isTraceEnabled) {
                log.trace(
                    "Add page. blockId: {}, pageId: {}, input: {},\npage: {}",
                    blockId,
                    savedPage.id,
                    input,
                    jsonService.toPrettyJson(page)
                )
            }

            return savedPage.id
        }
    }

    /**
     * Updates page by specified block or page
     */
    private fun updatePageExplicit(
        page: Page,
        blockId: Long,
        pageId: Long
    ): Long? {
        userState.getWriteLock().runIn {
            val block = userState.findBlockById(blockId)

            if (block == null) {
                log.warn("Block not found by id: {} while updating page: {}", blockId, page)
                return null
            }

            check(page.messageType == block.messageType) { "Update page message type mismatch block's type. Expected: ${block.messageType}" }

            val finalButtons = addBackButtonIf(page, blockId, true)
            when (page.messageType) {
                MessageType.Text -> {
                    messageSender.sendMessage(
                        chatId = input.chatId.toString(),
                        contentType = page.contentType,
                        disablePreview = page.disablePreview,
                        message = page.message,
                        preSendHandler = { msg ->
                            applyMessageButtons(msg, finalButtons, page.messageType)
                        })
                }

                MessageType.Inline -> {
                    messageSender.updateMessage(
                        chatId = input.chatId.toString(),
                        messageId = block.messageId,
                        contentType = page.contentType,
                        disablePreview = page.disablePreview,
                        message = page.message,
                        preSendHandler = { msg ->
                            applyMessageButtons(msg, finalButtons)
                        })
                }

                MessageType.Photo -> {
                    messageSender.updateImage(
                        chatId = input.chatId.toString(),
                        messageId = block.messageId,
                        file = page.file ?: error("MessageFile is required for photo message"),
                        caption = page.message,
                        captionContentType = page.contentType,
                        preSendHandler = { msg ->
                            applyMessageButtons(msg, finalButtons)
                        })
                }
            }

            val bestPageId = if (pageId == PAGE_ID_LAST)
                userState.getLastPage(blockId).id
            else
                pageId

            val savedPage = userState.savePage(
                blockId,
                pageId = bestPageId,
                handler = page.handler ?: controllerHandler.controllerClass,
                buttons = page.buttons
            )!!

            if (page.state != null) {
                saveLocalState(savedPage.id, page.state.items)
            }

            if (log.isTraceEnabled) {
                log.trace(
                    "Update page. blockId: {}, pageId: {}, input: {},\npage: {}",
                    blockId,
                    savedPage.id,
                    input,
                    jsonService.toPrettyJson(page)
                )
            }

            return savedPage.id
        }
    }

    /**
     * First page of the block cannot contain Back controller
     */
    private fun addBackButtonIf(page: Page, blockId: Long, isUpdate: Boolean): List<List<Button>> {
        if (page.enableBack == true) {
            // TODO: add special method to get pages count
            val pages = userState.getPages(blockId)
            val finalPageCount = pages.size + if (isUpdate) 0 else 1
            if (finalPageCount > 1) {
                return page.buttons + listOf(listOf(Button.GO_BACK))
            }
        }

        return page.buttons
    }

    override fun sendDocument(document: Document) {
        val chatId = if (document.chatId.isNotBlank()) document.chatId else input.chatId.toString()

        messageSender.sendDocument(
            chatId,
            file = document.file,
            caption = document.caption,
            captionContentType = document.captionContentType,
            disableNotification = document.disableNotification
        )
    }

    override fun sendImage(
        file: MessageFile,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    ): Int {
        return messageSender.sendImage(
            input.chatId.toString(),
            file,
            caption,
            captionContentType,
            disableNotification
        ) { }
    }

    override fun updateImage(
        messageId: Int,
        file: MessageFile,
        caption: String,
        captionContentType: ContentType
    ) {
        return messageSender.updateImage(input.chatId.toString(), messageId, file, caption, captionContentType) {}
    }

    override fun sendAdminMessage(message: String, contentType: ContentType, disablePreview: Boolean): Int {
        TODO("not implemented") //To change body of created functions use MessageFile | Settings | MessageFile Templates.
    }

    override fun sendMessage(message: String, contentType: ContentType, disablePreview: Boolean, chatId: String): Int {
        val finalChatId = if (chatId.isNotBlank()) chatId else input.chatId.toString()

        return messageSender.sendMessage(
            finalChatId,
            message,
            contentType = contentType,
            disablePreview = disablePreview,
            preSendHandler = {}
        )
    }

    override fun enterController(controller: BaseController) {
        TODO("not implemented") //To change body of created functions use MessageFile | Settings | MessageFile Templates.
    }

    override fun leaveController(controller: BaseController?) {
        TODO("not implemented") //To change body of created functions use MessageFile | Settings | MessageFile Templates.
    }

    override fun clearControllers() {
        TODO("not implemented") //To change body of created functions use MessageFile | Settings | MessageFile Templates.
    }

    override fun getTaskManager(): TaskManager = taskManager.value

    /**
     * Used when controller call another controller
     *
     * TODO: providing local state from caller
     */
    override fun executeTextMessage(clazz: Class<out BaseController>, text: String): Boolean {
        val newInput = input.copy(query = text, inlineMessageId = null, type = MessageType.Text)
        val handler = controllerHandlers.getControllerHandler(clazz)
        val states = userState.getStates()
        val context = createControllerContext(
            blockId = 0,
            currentMessageId = 0,
            controllerHandler = handler,
            states = states,
            messageType = MessageType.Text,
            input = newInput
        )
        return context.execute()
    }

    /**
     * Used when controller call another controller
     *
     * TODO: providing local state from caller
     */
    override fun executeInlineMessage(clazz: Class<out BaseController>, query: String): Boolean {
        val messageId = input.inlineMessageId
            ?: throw IllegalStateException("Inline message can be executed only in inline message context")
        val newInput = input.copy(query = query, inlineMessageId = messageId, type = MessageType.Inline)
        val handler = controllerHandlers.getControllerHandler(clazz.name)
        val states = userState.getStates()
        val context = createControllerContext(
            blockId = 0,
            currentMessageId = 0,
            controllerHandler = handler,
            states = states,
            messageType = MessageType.Text,
            input = newInput
        )
        return context.execute()
    }

    override fun <T : Service> getService(clazz: Class<T>): T = serviceProvider.getService(clazz)

    override fun <T : Service> tryGetService(clazz: Class<T>): T? = serviceProvider.tryGetService(clazz)

    override fun <T : UserService> getUserService(clazz: Class<T>): T =
        serviceProvider.getUserService(clazz, input.user.id)

    override fun <T : UserService> tryGetUserService(clazz: Class<T>): T? =
        serviceProvider.tryGetUserService(clazz, input.user.id)

    override fun page(message: String): PageBuilder = PageBuilderImpl(message, this)

    override fun page(file: MessageFile): PageBuilder = PageBuilderImpl("", this).file(file)

    override fun isAdmin(): Boolean = input.isAdmin

    override fun getUser(): InputUser = input.user

    private fun cloneFromBlock(blockId: Long, newMessageId: Int): MessagePage? {
        return userState.cloneFromBlock(blockId, newMessageId)
    }

    private fun findBlockIdByPageId(pageId: Long): Long? = userState.findBlockByPageId(pageId)?.id

    private fun createControllerContext(
        blockId: Long,
        currentMessageId: Int,
        messageType: MessageType,
        input: InputMessage,
        controllerHandler: ControllerHandler,
        states: States,
        pageId: Long = 0
    ): ControllerContext {
        return BaseContextImpl(
            blockId = blockId,
            pageId = pageId,
            messageType = messageType,
            currentMessageId = currentMessageId,
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

    /**
     * Creates [ControllerContext] for root controller
     */
    private fun getRootCallContext(): ControllerContext {
        val handler = controllerHandlers.getControllerHandler(rootController)
        val states = userState.getStates()
        return createControllerContext(
            blockId = 0,
            currentMessageId = input.messageId,
            messageType = MessageType.Text,
            controllerHandler = handler,
            states = states,
            input = input
        )
    }

    private fun saveLocalState(
        pageId: Long,
        stateItems: List<StateItem>
    ) {
        val localState = userState.getLocalStateProvider(pageId)
        localState.mergeAll(stateItems)
    }

    private fun validatePage(page: Page) {
        val pageHandler = page.handler

        if (pageHandler != null && page.messageType == MessageType.Text) {
            checkHandlerType(pageHandler, page.messageType)
        }

        val allButtons = page.buttons.flatten()
        if (allButtons.size > BUTTONS_MAX_SIZE) {
            error("Buttons size is too big: ${allButtons.size}. Max size is $BUTTONS_MAX_SIZE")
        }

        allButtons.filter { !it.isSystemMessage() }.forEach { subCmd ->
            val handler = subCmd.handler

            if (handler != null) {
                checkHandlerType(handler, page.messageType)
            } else if (page.messageType == MessageType.Inline) {
                val handler2 = page.handler ?: controllerHandler.controllerClass
                checkHandlerType(handler2, page.messageType)
            }
        }

        check(page.file == null || page.messageType in fileMessageTypes) {
            "File and message type are incompatible: ${page.messageType}. Expected one of the list: $fileMessageTypes"
        }
    }

    private fun checkHandlerType(
        handler: Class<out BaseController>,
        messageType: MessageType
    ) {
        val cmdHandler = controllerHandlers.getControllerHandler(handler)

        check(cmdHandler.canHandle(messageType)) {
            "Message handler for type $messageType in ${handler.name} not found. Use annotation @${
                annotationNameByType(
                    messageType
                )
            }"
        }
    }

    private fun getFinalMessageType(messageType: MessageType?, handler: ControllerHandler): MessageType {
        if (messageType != null) {
            return messageType
        }

        if (handler.canHandle(MessageType.Text)) {
            return MessageType.Text
        } else if (handler.canHandle(MessageType.Inline)) {
            return MessageType.Inline
        } else if (handler.canHandle(MessageType.Photo)) {
            return MessageType.Photo
        }

        error("Message handler for type ${handler.controllerClass.name} not found. Use one of the annotations: @TextHandler, @InlineHandler, etc.")
    }

    private fun annotationNameByType(messageType: MessageType) =
        when (messageType) {
            MessageType.Text -> "TextHandler"
            MessageType.Inline, MessageType.Photo -> "InlineHandler"
        }

    private fun applyMessageButtons(msg: SendMessage, buttons: List<List<Button>>, messageType: MessageType) {
        msg.replyMarkup = when (messageType) {
            MessageType.Inline, MessageType.Photo -> mapInlineKeyboardMarkup(buttons)
            MessageType.Text -> mapReplyKeyboardMarkup(buttons)
        }
    }

    private fun applyMessageButtons(msg: EditMessageText, buttons: List<List<Button>>) {
        msg.replyMarkup = mapInlineKeyboardMarkup(buttons)
    }

    private fun applyMessageButtons(msg: EditMessageMedia, buttons: List<List<Button>>) {
        msg.replyMarkup = mapInlineKeyboardMarkup(buttons)
    }

    private fun applyMessageButtons(msg: SendPhoto, buttons: List<List<Button>>) {
        msg.replyMarkup = mapInlineKeyboardMarkup(buttons)
    }

    private fun mapInlineKeyboardMarkup(buttons: List<List<Button>>): InlineKeyboardMarkup =
        InlineKeyboardMarkup().apply {
            keyboard = buttons.map { mapInlineButtonsRow(it) }
                .filter { it.isNotEmpty() }
                .toMutableList()
        }

    private fun mapReplyKeyboardMarkup(buttons: List<List<Button>>): ReplyKeyboardMarkup =
        ReplyKeyboardMarkup().apply {
            keyboard = buttons.map { mapTextButtonsRow(it) }
                .filter { it.isNotEmpty() }
                .toMutableList()
        }

    private fun mapTextButtonsRow(cmds: List<Button>): KeyboardRow =
        KeyboardRow().apply {
            cmds.forEach { cmd -> add(getTitle(cmd)) }
        }

    private fun mapInlineButtonsRow(cmds: List<Button>): MutableList<InlineKeyboardButton> =
        cmds.map { cmd ->
            InlineKeyboardButton().apply {
                text = getTitle(cmd)
                callbackData = cmd.titleId
            }
        }.toMutableList()

    private fun getTitle(cmd: Button) = cmd.title ?: localizationProvider.getString(cmd.titleId)

    private fun logContext() {
        if (log.isTraceEnabled) {
            log.trace(
                """
                    ----------------------------------------------------------
                    Controller: [{}]:{}
                    Handler: {}
                    Block/Page: {}/{}
                    State:
                      local: {}
                      shared: {}
                      user: {}
                      global: {}
                    ----------------------------------------------------------
                """.trimIndent(), input.type, input.query, controllerHandler.controller,
                blockId(),
                pageId(),
                states.getAll(StateKind.LOCAL),
                states.getAll(StateKind.SHARED),
                states.getAll(StateKind.USER),
                states.getAll(StateKind.GLOBAL)
            )
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(BaseContextImpl::class.java)!!
        const val PAGE_ID_LAST: Long = 0
        const val BUTTONS_MAX_SIZE = 100
        val fileMessageTypes = setOf(
            MessageType.Photo
        )
    }
}

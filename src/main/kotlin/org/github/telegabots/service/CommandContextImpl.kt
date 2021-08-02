package org.github.telegabots.service

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.CommandContext
import org.github.telegabots.api.ContentType
import org.github.telegabots.api.Document
import org.github.telegabots.api.InputMessage
import org.github.telegabots.api.InputUser
import org.github.telegabots.api.LocalizeProvider
import org.github.telegabots.api.MessageSender
import org.github.telegabots.api.MessageType
import org.github.telegabots.api.Page
import org.github.telegabots.api.Service
import org.github.telegabots.api.ServiceProvider
import org.github.telegabots.api.StateItem
import org.github.telegabots.api.SubCommand
import org.github.telegabots.api.SystemCommands
import org.github.telegabots.api.TaskManager
import org.github.telegabots.api.UserService
import org.github.telegabots.entity.CommandPage
import org.github.telegabots.state.UserStateService
import org.github.telegabots.task.TaskManagerFactory
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer

class CommandContextImpl(
    private val blockId: Long,
    private val pageId: Long,
    /**
     * Message id related with current block
     */
    private val currentMessageId: Int,
    private val input: InputMessage,
    private val command: BaseCommand,
    private val commandHandlers: CommandHandlers,
    private val userState: UserStateService,
    private val serviceProvider: ServiceProvider,
    private val localizeProvider: LocalizeProvider,
    private val messageSender: MessageSender,
    private val taskManagerFactory: TaskManagerFactory
) : CommandContext {
    private val log = LoggerFactory.getLogger(CommandContextImpl::class.java)!!
    private val jsonService = serviceProvider.getService(JsonService::class.java)!!
    private val taskManager = lazy { taskManagerFactory.create(blockId, pageId, input.user) }

    /**
     * Block was create while command executing
     *
     * Several updates can be made while one command executed
     */
    private val implicitBlockId = AtomicLong(0)

    override fun messageId(): Int = currentMessageId

    override fun inputMessageId(): Int = input.messageId

    override fun blockId(): Long = blockId

    override fun pageId(): Long = pageId

    override fun currentCommand(): BaseCommand = command

    override fun createPage(page: Page): Long {
        validatePageHandler(page)

        val messageId = messageSender.sendMessage(chatId = input.chatId.toString(),
            contentType = page.contentType,
            disablePreview = page.disablePreview,
            message = page.message,
            preSendHandler = Consumer { msg ->
                applyMessageButtons(msg, page.subCommands, page.messageType)
            })

        val block = userState.saveBlock(
            messageId = messageId,
            messageType = page.messageType
        )

        val savedPage = userState.savePage(
            block.id,
            handler = page.handler ?: command.javaClass,
            subCommands = page.subCommands
        )

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

    override fun addPage(page: Page): Long {
        validatePageHandler(page)

        if (page.blockId > 0) {
            return addPageExplicit(page, page.blockId)
        }

        val finalBlockId = implicitBlockId.get().let { if (it > 0) it else blockId }

        if (finalBlockId <= 0) {
            return createPage(page)
        }

        if (input.inlineMessageId == null && page.messageType == MessageType.Inline && implicitBlockId.get() <= 0) {
            val finalBlock = userState.getBlockById(finalBlockId)
            check(page.messageType == finalBlock.messageType) { "Adding page message type mismatch block's type. Expected: ${finalBlock.messageType}" }

            // from non-inline handler we have attempt to add page
            // we have to clone current block and bind it with new message
            val newMessageId = messageSender.sendMessage(chatId = input.chatId.toString(),
                contentType = page.contentType,
                disablePreview = page.disablePreview,
                message = page.message,
                preSendHandler = Consumer { msg ->
                    applyMessageButtons(msg, page.subCommands, page.messageType)
                })

            val lastPage = cloneFromBlock(blockId, newMessageId)
            implicitBlockId.set(lastPage.blockId)

            return addPageExplicit(page, implicitBlockId.get(), ignoreSender = true)
        }

        return addPageExplicit(page, finalBlockId)
    }

    override fun updatePage(page: Page): Long {
        validatePageHandler(page)

        if (page.blockId > 0) {
            return updatePageExplicit(page, page.blockId, finalPageId = page.id)
        }

        if (page.id > 0) {
            val blockByPageId = getBlockIdByPageId(page.id)
            return updatePageExplicit(page, blockByPageId, finalPageId = page.id)
        }

        val finalBlockId = implicitBlockId.get().let { if (it > 0) it else blockId }

        if (finalBlockId <= 0) {
            return createPage(page)
        }

        if (input.inlineMessageId == null && page.messageType == MessageType.Inline && implicitBlockId.get() <= 0) {
            val finalBlock = userState.getBlockById(finalBlockId)
            check(page.messageType == finalBlock.messageType) { "Update page message type mismatch block's type. Expected: ${finalBlock.messageType}" }

            // from non-inline handler we have attempt to update page
            // we have to clone current block and bind it with new message
            val newMessageId = messageSender.sendMessage(chatId = input.chatId.toString(),
                contentType = page.contentType,
                disablePreview = page.disablePreview,
                message = page.message,
                preSendHandler = Consumer { msg ->
                    applyMessageButtons(msg, page.subCommands, page.messageType)
                })

            val lastPage = cloneFromBlock(blockId, newMessageId)
            implicitBlockId.set(lastPage.blockId)

            return updatePageExplicit(page, implicitBlockId.get(), finalPageId = lastPage.id, ignoreSender = true)
        }

        val finalPageId = if (implicitBlockId.get() > 0) PAGE_ID_LAST else pageId

        return updatePageExplicit(page, finalBlockId, finalPageId = finalPageId)
    }

    override fun refreshPage(pageId: Long) {
        val page = userState.findPageById(pageId)

        if (page == null) {
            log.warn("Page not found by id: {}", pageId)
            return
        }

        val block = userState.getBlockById(page.blockId)

        if (block.messageType != MessageType.Inline) {
            log.warn("Page (id={}) can not be refreshed. Required Inline page, but found {}", pageId, block.messageType)
            return
        }

        val handler = commandHandlers.getCommandHandler(page.handler)
        val states = userState.getStates()
        val newInput = input.copy(
            type = MessageType.Inline,
            query = SystemCommands.REFRESH,
            messageId = block.messageId,
            inlineMessageId = block.messageId
        )
        val context = createCommandContext(
            blockId = block.id,
            currentMessageId = block.messageId,
            command = handler.command,
            input = newInput
        )
        val callContext = CommandCallContext(commandHandler = handler,
            input = newInput,
            states = states,
            commandContext = context,
            defaultContext = { null })

        callContext.execute()
    }

    override fun deletePage(pageId: Long) {
        val block = userState.findBlockByPageId(pageId)

        if (block != null) {
            val pages = userState.getPages(block.id)

            if (pages.size > 1) {
                userState.deletePage(block.id)

                if (block.messageType == MessageType.Inline) {
                    val lastPage = pages.last { it.id != pageId }
                    refreshPage(lastPage.id)
                }
            } else {
                userState.deleteBlock(block.id)
                messageSender.deleteMessage(input.chatId.toString(), block.messageId)
            }
        }
    }

    override fun deleteBlock(blockId: Long) {
        val block = userState.findBlockById(pageId)

        if (block != null) {
            userState.deleteBlock(block.id)
            messageSender.deleteMessage(input.chatId.toString(), block.messageId)
        }
    }

    override fun deleteMessage(messageId: Int) {
        val block = userState.findBlockByMessageId(messageId)

        if (block != null) {
            userState.deleteBlock(block.id)
        }

        messageSender.deleteMessage(input.chatId.toString(), messageId)
    }

    private fun addPageExplicit(
        page: Page,
        finalBlockId: Long,
        ignoreSender: Boolean = false,
    ): Long {
        val block = userState.getBlockById(finalBlockId)
        check(page.messageType == block.messageType) { "Adding page message type mismatch block's type. Expected: ${block.messageType}" }

        if (!ignoreSender) {
            when (page.messageType) {
                MessageType.Text -> {
                    messageSender.sendMessage(chatId = input.chatId.toString(),
                        contentType = page.contentType,
                        disablePreview = page.disablePreview,
                        message = page.message,
                        preSendHandler = Consumer { msg ->
                            applyMessageButtons(msg, page.subCommands, page.messageType)
                        })
                }
                MessageType.Inline -> {
                    val messageId: Int = block.messageId

                    messageSender.updateMessage(chatId = input.chatId.toString(),
                        messageId = messageId,
                        contentType = page.contentType,
                        disablePreview = page.disablePreview,
                        message = page.message,
                        preSendHandler = Consumer { msg ->
                            applyMessageButtons(msg, page.subCommands)
                        })
                }
            }
        }

        val savedPage = userState.savePage(
            blockId,
            handler = page.handler ?: command.javaClass,
            subCommands = page.subCommands
        )

        if (page.state != null) {
            saveLocalState(savedPage.id, page.state.items)
        }

        if (log.isDebugEnabled) {
            log.debug(
                "Add page. blockId: {}, pageId: {}, input: {},\npage: {}",
                blockId,
                savedPage.id,
                input,
                jsonService.toPrettyJson(page)
            )
        }

        return savedPage.id
    }

    private fun updatePageExplicit(
        page: Page,
        finalBlockId: Long,
        finalPageId: Long,
        ignoreSender: Boolean = false
    ): Long {
        val block = userState.getBlockById(finalBlockId)
        check(page.messageType == block.messageType) { "Update page message type mismatch block's type. Expected: ${block.messageType}" }

        if (!ignoreSender) {
            when (page.messageType) {
                MessageType.Text -> {
                    messageSender.sendMessage(chatId = input.chatId.toString(),
                        contentType = page.contentType,
                        disablePreview = page.disablePreview,
                        message = page.message,
                        preSendHandler = Consumer { msg ->
                            applyMessageButtons(msg, page.subCommands, page.messageType)
                        })
                }
                MessageType.Inline -> {
                    messageSender.updateMessage(chatId = input.chatId.toString(),
                        messageId = block.messageId,
                        contentType = page.contentType,
                        disablePreview = page.disablePreview,
                        message = page.message,
                        preSendHandler = Consumer { msg ->
                            applyMessageButtons(msg, page.subCommands)
                        })
                }
            }
        }

        val bestPageId = if (finalPageId == PAGE_ID_LAST)
            userState.getLastPage(finalBlockId).id
        else
            finalPageId

        val savedPage = userState.savePage(
            finalBlockId,
            pageId = bestPageId,
            handler = page.handler ?: command.javaClass,
            subCommands = page.subCommands
        )

        if (page.state != null) {
            saveLocalState(savedPage.id, page.state.items)
        }

        if (log.isDebugEnabled) {
            log.debug(
                "Update page. blockId: {}, pageId: {}, input: {},\npage: {}",
                blockId,
                savedPage.id,
                input,
                jsonService.toPrettyJson(page)
            )
        }

        return savedPage.id
    }

    override fun sendDocument(document: Document) {
        val chatId = if (document.chatId.isNotBlank()) document.chatId else input.chatId.toString()

        messageSender.sendDocument(
            chatId, file = document.file,
            caption = document.caption,
            captionContentType = document.captionContentType,
            disableNotification = document.disableNotification
        )
    }

    override fun sendAdminMessage(message: String, contentType: ContentType, disablePreview: Boolean): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun sendMessage(message: String, contentType: ContentType, disablePreview: Boolean, chatId: String): Int {
        val finalChatId = if (chatId.isNotBlank()) chatId else input.chatId.toString()

        return messageSender.sendMessage(
            finalChatId,
            message,
            contentType = contentType,
            disablePreview = disablePreview
        )
    }

    override fun enterCommand(command: BaseCommand) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun leaveCommand(command: BaseCommand?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clearCommands() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTaskManager(): TaskManager = taskManager.value

    /**
     * Used when command call another command
     *
     * TODO: providing local state from caller
     */
    override fun executeTextCommand(clazz: Class<out BaseCommand>, text: String): Boolean {
        val newInput = input.copy(query = text, inlineMessageId = null, type = MessageType.Text)
        val handler = commandHandlers.getCommandHandler(clazz)
        val states = userState.getStates()
        val context =
            createCommandContext(blockId = 0, currentMessageId = 0, command = handler.command, input = newInput)

        val callContext = CommandCallContext(commandHandler = handler,
            input = newInput,
            states = states,
            commandContext = context,
            defaultContext = { null })

        return callContext.execute()
    }

    /**
     * Used when command call another command
     *
     * TODO: providing local state from caller
     */
    override fun executeInlineCommand(clazz: Class<out BaseCommand>, query: String): Boolean {
        val messageId = input.inlineMessageId
            ?: throw IllegalStateException("Inline command can be executed only in inline message context")
        val newInput = input.copy(query = query, inlineMessageId = messageId, type = MessageType.Inline)
        val handler = commandHandlers.getCommandHandler(clazz.name)
        val states = userState.getStates()
        val context =
            createCommandContext(blockId = 0, currentMessageId = 0, command = handler.command, input = newInput)

        val callContext = CommandCallContext(commandHandler = handler,
            input = newInput,
            states = states,
            commandContext = context,
            defaultContext = { null })

        return callContext.execute()
    }

    override fun <T : Service> getService(clazz: Class<T>): T? = serviceProvider.getService(clazz)

    override fun <T : UserService> getUserService(clazz: Class<T>): T? =
        serviceProvider.getUserService(clazz, input.user)

    override fun isAdmin(): Boolean = input.isAdmin

    override fun user(): InputUser = input.user

    private fun cloneFromBlock(blockId: Long, newMessageId: Int): CommandPage {
        return userState.cloneFromBlock(blockId, newMessageId)
    }

    private fun getBlockIdByPageId(pageId: Long): Long =
        userState.findBlockByPageId(pageId)?.id ?: throw IllegalStateException("Block not found by pageId: $pageId")

    private fun createCommandContext(
        blockId: Long,
        currentMessageId: Int,
        command: BaseCommand,
        input: InputMessage,
        pageId: Long = 0
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
            localizeProvider = localizeProvider,
            userState = userState,
            taskManagerFactory = taskManagerFactory
        )
    }

    private fun saveLocalState(
        pageId: Long,
        stateItems: List<StateItem>
    ) {
        val localState = userState.getLocalStateProvider(pageId)
        localState.mergeAll(stateItems)
    }

    private fun validatePageHandler(page: Page) {
        val pageHandler = page.handler

        if (pageHandler != null && page.messageType == MessageType.Text) {
            checkHandlerType(pageHandler, page.messageType)
        }

        page.subCommands.flatten().filter { !it.isSystemCommand() }.forEach { subCmd ->
            val handler = subCmd.handler

            if (handler != null) {
                checkHandlerType(handler, page.messageType)
            } else if (page.messageType == MessageType.Inline) {
                val handler2 = page.handler ?: command.javaClass
                checkHandlerType(handler2, page.messageType)
            }
        }
    }

    private fun checkHandlerType(
        handler: Class<out BaseCommand>,
        messageType: MessageType
    ) {
        val cmdHandler = commandHandlers.getCommandHandler(handler)

        check(cmdHandler.canHandle(messageType)) {
            "Message handler for type ${messageType} in ${handler.name} not found. Use annotation @${
                annotationNameByType(
                    messageType
                )
            }"
        }
    }

    private fun annotationNameByType(messageType: MessageType) =
        when (messageType) {
            MessageType.Text -> "TextHandler"
            MessageType.Inline -> "InlineHandler"
        }

    private fun applyMessageButtons(msg: SendMessage, subCommands: List<List<SubCommand>>, messageType: MessageType) {
        msg.replyMarkup = when (messageType) {
            MessageType.Inline -> mapInlineKeyboardMarkup(subCommands)
            MessageType.Text -> mapReplyKeyboardMarkup(subCommands)
        }
    }

    private fun applyMessageButtons(msg: EditMessageText, subCommands: List<List<SubCommand>>) {
        msg.replyMarkup = mapInlineKeyboardMarkup(subCommands)
    }

    private fun mapInlineKeyboardMarkup(subCommands: List<List<SubCommand>>): InlineKeyboardMarkup =
        InlineKeyboardMarkup().setKeyboard(subCommands.map { mapInlineButtonsRow(it) }
            .filter { it.isNotEmpty() }
            .toMutableList())

    private fun mapReplyKeyboardMarkup(subCommands: List<List<SubCommand>>): ReplyKeyboardMarkup =
        ReplyKeyboardMarkup()
            .setKeyboard(subCommands.map { mapTextButtonsRow(it) }
                .filter { it.isNotEmpty() }
                .toMutableList())

    private fun mapTextButtonsRow(cmds: List<SubCommand>): KeyboardRow =
        KeyboardRow().apply {
            cmds.forEach { cmd -> add(getTitle(cmd)) }
        }

    private fun mapInlineButtonsRow(cmds: List<SubCommand>): MutableList<InlineKeyboardButton> =
        cmds.map { cmd ->
            InlineKeyboardButton()
                .setText(getTitle(cmd))
                .setCallbackData(cmd.titleId)
        }.toMutableList()

    private fun getTitle(cmd: SubCommand) = cmd.title ?: localizeProvider.getString(cmd.titleId)

    companion object {
        private const val PAGE_ID_LAST: Long = 0
    }
}

package org.github.telegabots.service

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.BlockInfo
import org.github.telegabots.api.BlockStateInfo
import org.github.telegabots.api.CommandContext
import org.github.telegabots.api.ContentType
import org.github.telegabots.api.Document
import org.github.telegabots.api.InputMessage
import org.github.telegabots.api.InputUser
import org.github.telegabots.api.LocalizeProvider
import org.github.telegabots.api.MessageSender
import org.github.telegabots.api.MessageType
import org.github.telegabots.api.Page
import org.github.telegabots.api.PageInfo
import org.github.telegabots.api.PageStateInfo
import org.github.telegabots.api.Service
import org.github.telegabots.api.ServiceProvider
import org.github.telegabots.api.StateItem
import org.github.telegabots.api.StateRef
import org.github.telegabots.api.SubCommand
import org.github.telegabots.api.SystemCommands
import org.github.telegabots.api.TaskContext
import org.github.telegabots.api.TaskManager
import org.github.telegabots.api.UserService
import org.github.telegabots.entity.CommandBlock
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
import java.util.function.Consumer

class BaseContextImpl(
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
) : CommandContext, TaskContext {
    private val log = LoggerFactory.getLogger(BaseContextImpl::class.java)!!
    private val jsonService = serviceProvider.getService(JsonService::class.java)!!
    private val taskManager = lazy { taskManagerFactory.create(this) }

    override fun inputMessage(): InputMessage = input

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
            preSendHandler = { msg ->
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

        if (blockId <= 0) {
            return createPage(page)
        }

        return addPageExplicit(page, blockId)
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

        if (blockId <= 0) {
            return createPage(page)
        }

        return updatePageExplicit(page, blockId, finalPageId = pageId)
    }

    override fun refreshPage(pageId: Long, state: StateRef?) {
        val finalPageId = if (pageId > 0) pageId else pageId()
        val page = userState.findPageById(finalPageId)

        if (page == null) {
            log.warn("Page not found by id: {}", finalPageId)
            return
        }

        val block = userState.getBlockById(page.blockId)

        if (block.messageType != MessageType.Inline) {
            log.warn(
                "Page (id={}) can not be refreshed. Required Inline page, but found {}",
                finalPageId,
                block.messageType
            )
            return
        }

        val handler = commandHandlers.getCommandHandler(page.handler)
        // TODO: improve userState to not use jsonService.toStateDef
        val states =
            userState.getStates(
                messageId = block.messageId,
                pageId = finalPageId,
                state = jsonService.toStateDef(state)
            )
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
        val callContext = CommandCallContextImpl(commandHandler = handler,
            states = states,
            commandContext = context,
            defaultContext = { null })

        callContext.execute()
    }

    override fun deletePage(pageId: Long) {
        val finalPageId = if (pageId > 0) pageId else pageId()
        val block = userState.findBlockByPageId(finalPageId)

        if (block != null) {
            val pages = userState.getPages(block.id)

            if (pages.size > 1) {
                userState.deletePage(block.id)

                if (block.messageType == MessageType.Inline) {
                    val lastPage = pages.last { it.id != finalPageId }
                    refreshPage(lastPage.id)
                }
            } else {
                userState.deleteBlock(block.id)
                messageSender.deleteMessage(input.chatId.toString(), block.messageId)
            }
        }
    }

    override fun deleteBlock(blockId: Long) {
        val finalBlockId = if (blockId > 0) blockId else blockId()
        val block = userState.findBlockById(finalBlockId)

        if (block != null) {
            userState.deleteBlock(block.id)
            messageSender.deleteMessage(input.chatId.toString(), block.messageId)
        } else {
            log.warn("Block not found by id: {}", finalBlockId)
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
        val block = userState.findBlockByPageId(finalPageId)
        val lastPage = block?.id?.let { userState.findLastPage(it) }

        return finalPageId != 0L && lastPage?.id == finalPageId
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

    private fun mapBlockInfo(it: CommandBlock) = BlockInfo(it.id, createdAt = it.createdAt)

    private fun mapPageInfo(it: CommandPage): PageInfo =
        PageInfo(it.id, createdAt = it.createdAt, updatedAt = it.updatedAt)

    /**
     * Adds page to specified block
     */
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

    /**
     * Updates page by specified block or page
     */
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

        val callContext = CommandCallContextImpl(commandHandler = handler,
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

        val callContext = CommandCallContextImpl(commandHandler = handler,
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

package org.github.telegabots.service

import org.github.telegabots.api.*
import org.github.telegabots.state.UserStateService
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import java.lang.IllegalStateException
import java.util.function.Consumer

class CommandContextImpl(
    private val blockId: Long,
    private val pageId: Long,
    private val input: InputMessage,
    private val command: BaseCommand,
    private val commandHandlers: CommandHandlers,
    private val userState: UserStateService,
    private val serviceProvider: ServiceProvider,
    private val localizeProvider: LocalizeProvider,
    private val messageSender: MessageSender
) : CommandContext {
    private val log = LoggerFactory.getLogger(CommandContextImpl::class.java)!!
    private val jsonService = serviceProvider.getService(JsonService::class.java)!!

    override fun inlineMessageId(): Int? = input.messageId

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
            subCommands = page.subCommands,
            messageId = when (page.messageType) {
                MessageType.Text -> messageId
                MessageType.Inline -> null
            }
        )

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
        if (blockId <= 0) {
            return createPage(page)
        }

        validatePageHandler(page)
        val block = userState.getBlockById(blockId) ?: throw IllegalStateException("Block by id not found: $blockId")
        check(page.messageType == block.messageType) { "Adding page message type mismatch block's type. Expected: ${block.messageType}" }

        val resultMessageId = when (page.messageType) {
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
                check(input.messageId ?: 0 > 0) { "Input message id cannot be null or negative. Input: $input" }
                val messageId: Int = input.messageId!!

                messageSender.updateMessage(chatId = input.chatId.toString(),
                    messageId = messageId,
                    contentType = page.contentType,
                    disablePreview = page.disablePreview,
                    message = page.message,
                    preSendHandler = Consumer { msg ->
                        applyMessageButtons(msg, page.subCommands)
                    })

                null
            }
        }

        val savedPage = userState.savePage(
            blockId,
            messageId = resultMessageId,
            handler = page.handler ?: command.javaClass,
            subCommands = page.subCommands
        )

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

    override fun updatePage(page: Page): Long {
        if (blockId <= 0) {
            return createPage(page)
        }

        validatePageHandler(page)
        val block = userState.getBlockById(blockId) ?: throw IllegalStateException("Block by id not found: $blockId")
        check(page.messageType == block.messageType) { "Adding page message type mismatch block's type. Expected: ${block.messageType}" }

        val resultMessageId = when (page.messageType) {
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
                check(input.messageId ?: 0 > 0) { "Input message id cannot be null or negative. Input: $input" }
                val messageId: Int = input.messageId!!

                messageSender.updateMessage(chatId = input.chatId.toString(),
                    messageId = messageId,
                    contentType = page.contentType,
                    disablePreview = page.disablePreview,
                    message = page.message,
                    preSendHandler = Consumer { msg ->
                        applyMessageButtons(msg, page.subCommands)
                    })
                null
            }
        }

        val savedPage = userState.savePage(
            blockId,
            messageId = resultMessageId,
            pageId = if (page.id > 0) page.id else pageId,
            handler = page.handler ?: command.javaClass,
            subCommands = page.subCommands
        )

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

    /**
     * Used when command call another command
     *
     * TODO: providing local state from caller
     */
    override fun executeTextCommand(clazz: Class<out BaseCommand>, text: String): Boolean {
        val newInput = input.copy(query = text, messageId = null, type = MessageType.Text)
        val handler = commandHandlers.getCommandHandler(clazz)
        val states = userState.getStates()
        val context = createCommandContext(blockId = 0, command = handler.command, input = newInput)

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
        val messageId = input.messageId
            ?: throw IllegalStateException("Inline command can be executed only in inline message context")
        val newInput = input.copy(query = query, messageId = messageId, type = MessageType.Inline)
        val handler = commandHandlers.getCommandHandler(clazz.name)
        val states = userState.getStates()
        val context = createCommandContext(blockId = 0, command = handler.command, input = newInput)

        val callContext = CommandCallContext(commandHandler = handler,
            input = newInput,
            states = states,
            commandContext = context,
            defaultContext = { null })

        return callContext.execute()
    }

    override fun <T : Service> getService(clazz: Class<T>): T? = serviceProvider.getService(clazz)

    override fun userId(): Int = input.userId

    override fun isAdmin(): Boolean = input.isAdmin

    private fun createCommandContext(
        blockId: Long,
        command: BaseCommand,
        input: InputMessage,
        pageId: Long = 0
    ): CommandContext {
        return CommandContextImpl(
            blockId = blockId,
            pageId = pageId,
            command = command,
            input = input,
            commandHandlers = commandHandlers,
            messageSender = messageSender,
            serviceProvider = serviceProvider,
            localizeProvider = localizeProvider,
            userState = userState
        )
    }

    private fun validatePageHandler(page: Page) {
        val handler = page.handler ?: command.javaClass
        val cmdHandler = commandHandlers.getCommandHandler(handler)

        check(cmdHandler.canHandle(page.messageType)) {
            "Message handler for type ${page.messageType} in ${handler.name} not found. Use annotation @${
                annotationNameByType(
                    page.messageType
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
}

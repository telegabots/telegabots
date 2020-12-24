package org.github.telegabots.service

import org.github.telegabots.*
import org.github.telegabots.state.UserStateService
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
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
    override fun currentCommand(): BaseCommand = command

    override fun createPage(page: Page): Long {
        log.debug("Create page: {} by input: {}, blockId: {}", page, input, blockId)

        val messageId = messageSender.sendMessage(chatId = input.chatId.toString(),
            contentType = page.contentType,
            disablePreview = page.disablePreview,
            message = page.message,
            preSendHandler = Consumer { msg ->
                applyTextButtons(msg, page.subCommands)
            })

        val block = userState.saveBlock(
            messageId = messageId,
            messageType = page.messageType
        )

        val savedPage = userState.savePage(
            block.id,
            handler = page.handler ?: command::class.java,
            subCommands = page.subCommands
        )

        return savedPage.id
    }

    override fun addPage(page: Page): Long {
        if (blockId <= 0) {
            return createPage(page)
        }

        log.debug("Add page: {} by input: {}, blockId: {}", page, input, blockId)

        val messageId: Int = checkNotNull(input.messageId) { "Input message id cannot be null. Input: $input" }

        messageSender.updateMessage(chatId = input.chatId.toString(),
            messageId = messageId,
            contentType = page.contentType,
            disablePreview = page.disablePreview,
            message = page.message,
            preSendHandler = Consumer { msg ->
                TODO("applyCallbackButtons()")
            })

        val savedPage = userState.savePage(
            blockId,
            handler = page.handler ?: command::class.java,
            subCommands = page.subCommands
        )

        return savedPage.id
    }

    override fun updatePage(page: Page): Long {
        if (blockId <= 0) {
            return createPage(page)
        }

        log.debug("Update page: {} by input: {}, blockId: {}", page, input, blockId)

        val messageId: Int = checkNotNull(input.messageId) { "Input message id cannot be null. Input: $input" }

        messageSender.updateMessage(chatId = input.chatId.toString(),
            messageId = messageId,
            contentType = page.contentType,
            disablePreview = page.disablePreview,
            message = page.message,
            preSendHandler = Consumer { msg ->
                TODO("applyCallbackButtons()")
            })

        val savedPage = userState.savePage(
            blockId,
            pageId = if (page.id > 0) page.id else pageId,
            handler = page.handler ?: command::class.java,
            subCommands = page.subCommands
        )

        return savedPage.id
    }

    override fun sendAdminMessage(message: String, contentType: ContentType, disablePreview: Boolean): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
    override fun executeCommand(clazz: Class<out BaseCommand>, text: String): Boolean {
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
    override fun executeCallback(clazz: Class<out BaseCommand>, messageId: Int, query: String): Boolean {
        val newInput = input.copy(query = query, messageId = messageId, type = MessageType.Callback)
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

    override fun <T : Service> getService(clazz: Class<T>): T = serviceProvider.getService(clazz)

    override fun userId(): Int = input.userId

    override fun isAdmin(): Boolean = input.isAdmin

    private fun createCommandContext(blockId: Long, command: BaseCommand, input: InputMessage, pageId: Long = 0): CommandContext {
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

    private fun applyCallbackButtons(msg: EditMessageText, subCommands: List<List<SubCommand>>) {
        val keyboardMarkup = InlineKeyboardMarkup()
        msg.replyMarkup = keyboardMarkup
        keyboardMarkup.keyboard = mapCallbackButtons(subCommands)
    }

    private fun applyTextButtons(msg: SendMessage, subCommands: List<List<SubCommand>>) {
        val keyboardMarkup = ReplyKeyboardMarkup()
        msg.replyMarkup = keyboardMarkup
        keyboardMarkup.keyboard = mapTextButtons(subCommands)
    }

    private fun mapTextButtons(subCommands: List<List<SubCommand>>): MutableList<KeyboardRow> =
        subCommands.map { mapTextButtonsRow(it) }
            .filter { it.isNotEmpty() }
            .toMutableList()

    private fun mapCallbackButtons(subCommands: List<List<SubCommand>>): MutableList<MutableList<InlineKeyboardButton>> =
        subCommands.map { mapCallbackButtonsRow(it) }
            .filter { it.isNotEmpty() }
            .toMutableList()

    private fun mapTextButtonsRow(cmds: List<SubCommand>): KeyboardRow =
        KeyboardRow().apply {
            cmds.forEach { cmd -> add(localizeProvider.getString(cmd.titleId)) }
        }

    private fun mapCallbackButtonsRow(cmds: List<SubCommand>): MutableList<InlineKeyboardButton> =
        cmds.map { cmd ->
            InlineKeyboardButton()
                .setText(localizeProvider.getString(cmd.titleId))
                .setCallbackData(cmd.titleId)
        }.toMutableList()
}

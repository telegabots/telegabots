package org.github.telegabots.service

import org.github.telegabots.*
import org.github.telegabots.state.UserStateService
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import java.util.function.Consumer

class CommandContextImpl(
    private val blockId: Long,
    private val input: InputMessage,
    private val command: BaseCommand,
    private val commandHandlers: CommandHandlers,
    private val userState: UserStateService,
    private val serviceProvider: ServiceProvider,
    private val localizeProvider: LocalizeProvider,
    private val messageSender: MessageSender
) : CommandContext {
    override fun sendMessage(
        message: String,
        contentType: ContentType,
        messageType: MessageType,
        disablePreview: Boolean,
        subCommands: List<List<SubCommand>>,
        handler: Class<out BaseCommand>?
    ) {
        val messageId = messageSender.sendMessage(chatId = input.chatId.toString(),
            contentType = contentType,
            disablePreview = disablePreview,
            message = message,
            preSendHandler = Consumer { msg ->
                applyButtons(msg, messageType, subCommands)
            })

        val finalBlockId = if (blockId > 0) blockId
        else {
            val block = userState.saveBlock(
                messageId = messageId,
                messageType = input.type
            )
            block.id
        }

        userState.savePage(finalBlockId, handler = handler ?: command::class.java, subCommands = subCommands)
    }

    override fun updateMessage(
        messageId: Int,
        message: String,
        contentType: ContentType,
        updateType: UpdateType,
        disablePreview: Boolean,
        subCommands: List<List<SubCommand>>,
        handler: Class<out BaseCommand>?
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun currentCommand(): BaseCommand = command


    override fun sendAdminMessage(message: String, contentType: ContentType, disablePreview: Boolean) {
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

    private fun createCommandContext(blockId: Long, command: BaseCommand, input: InputMessage): CommandContext {
        return CommandContextImpl(
            blockId = blockId,
            command = command,
            input = input,
            commandHandlers = commandHandlers,
            messageSender = messageSender,
            serviceProvider = serviceProvider,
            localizeProvider = localizeProvider,
            userState = userState
        )
    }

    private fun applyButtons(
        msg: SendMessage,
        messageType: MessageType,
        subCommands: List<List<SubCommand>>
    ) {
        when (messageType) {
            MessageType.Text -> applyTextButtons(msg, subCommands)
            MessageType.Callback -> applyCallbackButtons(msg, subCommands)
        }
    }

    private fun applyCallbackButtons(msg: SendMessage, subCommands: List<List<SubCommand>>) {
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

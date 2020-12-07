package org.github.telegabots

import org.slf4j.LoggerFactory
import org.github.telegabots.service.CallContextManager
import org.github.telegabots.service.CommandHandlers
import org.github.telegabots.service.JsonService
import org.github.telegabots.state.StateDbProvider
import org.github.telegabots.state.UsersStatesManager
import org.telegram.telegrambots.meta.api.objects.Update

/**
 * Wrapper for convenient Telegram bot running
 */
class TelegaBot(
    private val messageSender: MessageSender,
    private val serviceProvider: ServiceProvider,
    private val adminChatId: Long = 0L,
    private val dbProvider: StateDbProvider,
    private val jsonService: JsonService,
    private val rootCommand: Class<out BaseCommand> = EmptyCommand::class.java
) {
    private val log = LoggerFactory.getLogger(TelegaBot::class.java)
    private val commandHandlers = CommandHandlers()
    private val usersStatesManager = UsersStatesManager(dbProvider, jsonService)
    private val callContextManager = CallContextManager(
        messageSender, serviceProvider, commandHandlers, usersStatesManager, rootCommand
    )

    fun handle(update: Update): Boolean {
        log.debug("Handle message: {}", update)

        val inputMessage = getInputMessage(update)

        if (inputMessage == null) {
            TODO("TODO: send message to admin. Failed handle '$update'")
        }

        val context = callContextManager.get(inputMessage)

        return context.execute()
    }

    private fun getInputMessage(update: Update): InputMessage? {
        return if (update.hasMessage() && update.message.hasText()) {
            val message = update.message
            val userId = message.from.id

            InputMessage(
                type = MessageType.TEXT,
                query = message.text,
                chatId = message.chatId,
                userId = userId,
                isAdmin = userId.toLong() == adminChatId
            )
        } else if (update.hasCallbackQuery()) {
            val callbackQuery = update.callbackQuery
            val message = callbackQuery.message
            val userId = message.from.id

            InputMessage(
                type = MessageType.CALLBACK,
                query = callbackQuery.data ?: "",
                chatId = message.chatId,
                userId = userId,
                messageId = message.messageId,
                isAdmin = userId.toLong() == adminChatId
            )
        } else {
            log.warn("Unsupported message type: {}", update)
            return null
        }
    }
}

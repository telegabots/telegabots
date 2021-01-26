package org.github.telegabots.api

import org.github.telegabots.service.*
import org.github.telegabots.state.StateDbProvider
import org.github.telegabots.state.UsersStatesManager
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.objects.Update

/**
 * Wrapper for convenient Telegram bot running
 */
class TelegaBot(
    private val messageSender: MessageSender,
    private val serviceProvider: ServiceProvider,
    private val adminChatId: Long = 0L,
    private val dbProvider: StateDbProvider,
    private val rootCommand: Class<out BaseCommand> = EmptyCommand::class.java,
    private val commandInterceptor: CommandInterceptor = CommandInterceptorEmpty
) {
    private val log = LoggerFactory.getLogger(TelegaBot::class.java)
    private val jsonService: JsonService = JsonService()
    private val commandHandlers = CommandHandlers(commandInterceptor)
    private val userLocalizationFactory =
        serviceProvider.getService(UserLocalizationFactory::class.java) ?: FileBasedLocalizationFactory(jsonService)
    private val usersStatesManager = UsersStatesManager(dbProvider, userLocalizationFactory, jsonService)
    private val callContextManager = CallContextManager(
        messageSender, serviceProvider, commandHandlers, usersStatesManager, userLocalizationFactory, rootCommand
    )

    fun handle(update: Update): Boolean {
        log.debug("Handle message: {}", update)

        val inputMessage = getInputMessage(update)

        if (inputMessage == null) {
            TODO("TODO: send message to admin. Failed handle '$update'")
        }

        val context = callContextManager.get(inputMessage)

        log.debug("Got context by message: {}, context: {}", inputMessage, context)

        return context.execute()
    }

    private fun getInputMessage(update: Update): InputMessage? {
        return if (update.hasMessage() && update.message.hasText()) {
            val message = update.message
            val userId = message.from.id

            InputMessage(
                type = MessageType.Text,
                query = message.text,
                chatId = message.chatId,
                userId = userId,
                isAdmin = userId.toLong() == adminChatId
            )
        } else if (update.hasCallbackQuery()) {
            val callbackQuery = update.callbackQuery
            val message = callbackQuery.message
            val userId = callbackQuery.from.id

            InputMessage(
                type = MessageType.Inline,
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
package org.github.telegabots.api

import org.github.telegabots.api.config.BotConfig
import org.github.telegabots.service.CommandCallContextFactory
import org.github.telegabots.service.InternalServiceProvider
import org.github.telegabots.service.JsonService
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User

/**
 * Wrapper for convenient Telegram bot running
 */
class TelegaBot(
    messageSender: MessageSender,
    userServiceProvider: ServiceProvider,
    val config: BotConfig,
    val rootCommand: Class<out BaseCommand> = EmptyCommand::class.java
) {
    private val log = LoggerFactory.getLogger(TelegaBot::class.java)
    private val adminChatId: Long = config.adminChatId
    private val jsonService: JsonService = JsonService()
    private val finalServiceProvider = InternalServiceProvider(userServiceProvider, messageSender, jsonService, config)
    private val callContextManager = CommandCallContextFactory(finalServiceProvider, rootCommand)

    fun handle(update: Update): Boolean {
        log.debug("Handle message: {}", update)

        val inputMessage = getInputMessage(update)

        if (inputMessage == null) {
            TODO("TODO: send message to admin. Failed handle '$update'")
        }

        val context = callContextManager.get(inputMessage)

        log.debug("Got context by message: {}\ncontext: {}", inputMessage, context)

        return context.execute()
    }

    fun <T : Service> getService(clazz: Class<T>): T = finalServiceProvider.getService(clazz)

    fun <T : Service> tryGetService(clazz: Class<T>): T? = finalServiceProvider.tryGetService(clazz)

    private fun getInputMessage(update: Update): InputMessage? {
        return if (update.hasMessage() && update.message.hasText()) {
            val message = update.message
            val user = message.from!!
            val userId = user.id

            InputMessage(
                type = MessageType.Text,
                query = message.text,
                chatId = message.chatId,
                userId = userId,
                user = toUser(user),
                messageId = message.messageId,
                inlineMessageId = null,
                isAdmin = userId.toLong() == adminChatId
            )
        } else if (update.hasCallbackQuery()) {
            val callbackQuery = update.callbackQuery
            val message = callbackQuery.message
            val user = callbackQuery.from!!
            val userId = callbackQuery.from.id

            InputMessage(
                type = MessageType.Inline,
                query = callbackQuery.data ?: "",
                chatId = message.chatId,
                userId = userId,
                user = toUser(user),
                messageId = message.messageId,
                inlineMessageId = message.messageId,
                isAdmin = userId == adminChatId
            )
        } else {
            log.warn("Unsupported message type: {}", update)
            return null
        }
    }

    private fun toUser(user: User): InputUser =
        InputUser(
            user.id, firstName = user.firstName ?: "",
            lastName = user.lastName ?: "",
            userName = user.userName ?: "",
            isBot = user.isBot
        )
}

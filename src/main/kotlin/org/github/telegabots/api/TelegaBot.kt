package org.github.telegabots.api

import org.github.telegabots.api.config.BotConfig
import org.github.telegabots.service.AlertServiceImpl
import org.github.telegabots.service.CallContextManager
import org.github.telegabots.service.CommandHandlers
import org.github.telegabots.service.InternalServiceProvider
import org.github.telegabots.service.JsonService
import org.github.telegabots.state.InternalLockableStateDbProvider
import org.github.telegabots.state.LockableStateDbProvider
import org.github.telegabots.state.StateDbProvider
import org.github.telegabots.state.UsersStatesManager
import org.github.telegabots.util.CommandValidatorImpl
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User

/**
 * Wrapper for convenient Telegram bot running
 */
class TelegaBot(
    private val messageSender: MessageSender,
    private val serviceProvider: ServiceProvider,
    private val config: BotConfig,
    private val dbProvider: StateDbProvider,
    private val rootCommand: Class<out BaseCommand> = EmptyCommand::class.java,
    private val commandInterceptor: CommandInterceptor = CommandInterceptorEmpty
) {
    private val log = LoggerFactory.getLogger(TelegaBot::class.java)
    private val adminChatId: Long = config.adminChatId
    private val jsonService: JsonService = JsonService()
    private val commandHandlers = CommandHandlers(commandInterceptor)
    private val commandValidator = CommandValidatorImpl(commandHandlers)
    private val finalDbProvider: LockableStateDbProvider =
        if (dbProvider is LockableStateDbProvider) dbProvider else InternalLockableStateDbProvider(dbProvider)
    private val finalServiceProvider = InternalServiceProvider(serviceProvider, jsonService)
    private val userLocalizationFactory = finalServiceProvider.getService(UserLocalizationFactory::class.java)!!
    private val usersStatesManager = UsersStatesManager(finalDbProvider, userLocalizationFactory, jsonService)
    private val callContextManager = CallContextManager(
        messageSender,
        finalServiceProvider,
        commandHandlers,
        usersStatesManager,
        userLocalizationFactory,
        rootCommand
    )
    private val alertService = AlertServiceImpl(messageSender, config.alertChatId)

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

    fun <T : Service> getService(clazz: Class<T>): T? {
        val service = when (clazz) {
            CommandValidator::class.java -> commandValidator
            MessageSender::class.java -> messageSender
            AlertService::class.java -> alertService
            else -> null
        }

        return service as T?
    }

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
                isAdmin = userId.toLong() == adminChatId
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
            isBot = user.bot == true
        )
}

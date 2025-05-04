package org.github.telegabots.api

import org.github.telegabots.api.config.BotConfig
import org.github.telegabots.service.*
import org.github.telegabots.service.CommandConextFactory
import org.github.telegabots.service.CommandHandlers
import org.github.telegabots.service.InternalServiceProvider
import org.github.telegabots.task.TaskManagerFactory
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
    private val adminChatId: Long = config.adminChatId
    private val jsonService: JsonService = JsonService()
    private val finalServiceProvider = InternalServiceProvider(userServiceProvider, messageSender, jsonService, config)
    private val taskManagerFactory = finalServiceProvider.getService(TaskManagerFactory::class.java)
    private val messageSender = finalServiceProvider.getService(MessageSender::class.java)
    private val commandHandlers = finalServiceProvider.getService(CommandHandlers::class.java)

    init {
        val rootHandler = commandHandlers.getCommandHandler(rootCommand)

        check(rootHandler.canHandle(MessageType.Text)) { "Root command (${rootCommand.name}) have to implement text handler. Annotate method with @TextHandler" }

        log.info("CommandCallContextFactory created")
    }

    fun handle(update: Update): Boolean {
        log.debug("Handle message: {}", update)

        val inputMessage = getInputMessage(update)

        val context = createContext(inputMessage)

        log.debug("Got context by message: {}\ncontext: {}", inputMessage, context)

        return context.execute()
    }

    fun <T : Service> getService(clazz: Class<T>): T = finalServiceProvider.getService(clazz)

    fun <T : Service> tryGetService(clazz: Class<T>): T? = finalServiceProvider.tryGetService(clazz)

    private fun createContext(input: InputMessage): CommandContext = CommandConextFactory(
        input,
        messageSender,
        finalServiceProvider,
        commandHandlers,
        taskManagerFactory,
        rootCommand
    ).create()

    private fun getInputMessage(update: Update): InputMessage {
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
                isAdmin = userId == adminChatId
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
            // TODO: send message to admin
            error("Unsupported message type: $update")
        }
    }

    private fun toUser(user: User): InputUser =
        InputUser(
            user.id,
            firstName = user.firstName ?: "",
            lastName = user.lastName ?: "",
            userName = user.userName ?: "",
            isBot = user.isBot
        )

    private companion object {
        private val log = LoggerFactory.getLogger(TelegaBot::class.java)!!
    }
}

package org.github.telegabots.api

import org.github.telegabots.api.config.BotConfig
import org.github.telegabots.service.*
import org.github.telegabots.service.ControllerContextFactory
import org.github.telegabots.service.ControllerHandlers
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
    val rootController: Class<out BaseController> = EmptyController::class.java
) {
    private val adminChatId: Long = config.adminChatId
    private val jsonService: InternalJsonService = InternalJsonService()
    private val finalServiceProvider = InternalServiceProvider(userServiceProvider, messageSender, jsonService, config)
    private val taskManagerFactory = finalServiceProvider.getService(TaskManagerFactory::class.java)
    private val messageSender = finalServiceProvider.getService(MessageSender::class.java)
    private val controllerHandlers = finalServiceProvider.getService(ControllerHandlers::class.java)

    init {
        val rootHandler = controllerHandlers.getControllerHandler(rootController)

        check(rootHandler.canHandle(MessageType.Text)) { "Root controller (${rootController.name}) have to implement text handler. Annotate method with @TextHandler" }

        log.info("TelegaBot created")
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

    private fun createContext(input: InputMessage): ControllerContext = ControllerContextFactory(
        input,
        messageSender,
        finalServiceProvider,
        controllerHandlers,
        taskManagerFactory,
        rootController
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
        val log = LoggerFactory.getLogger(TelegaBot::class.java)!!
    }
}

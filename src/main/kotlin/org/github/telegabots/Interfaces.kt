package org.github.telegabots

import org.github.telegabots.state.State
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import java.util.function.Consumer

/**
 * Context used by a command
 */
interface CommandContext : UserContext, CommandExecutor {
    fun currentCommand(): BaseCommand

    fun sendMessage(
        message: String,
        contentType: ContentType,
        messageType: MessageType,
        disablePreview: Boolean = true,
        subCommands: List<List<SubCommand>> = emptyList(),
        handler: Class<out BaseCommand>? = null
    )

    fun updateMessage(
        messageId: Int,
        message: String,
        contentType: ContentType,
        updateType: UpdateType,
        disablePreview: Boolean = true,
        subCommands: List<List<SubCommand>> = emptyList(),
        handler: Class<out BaseCommand>? = null
    )

    fun sendAdminMessage(
        message: String,
        contentType: ContentType,
        disablePreview: Boolean = true
    )

    fun sendHtmlMessage(
        message: String,
        messageType: MessageType = MessageType.TEXT,
        disablePreview: Boolean = true,
        subCommands: List<List<SubCommand>> = emptyList(),
        handler: Class<out BaseCommand>? = null
    ) =
        sendMessage(
            message,
            contentType = ContentType.Html,
            messageType = messageType,
            disablePreview = disablePreview,
            subCommands = subCommands,
            handler = handler
        )

    fun updateHtmlMessage(
        messageId: Int,
        message: String,
        updateType: UpdateType,
        disablePreview: Boolean = true,
        subCommands: List<List<SubCommand>> = emptyList(),
        handler: Class<out BaseCommand>? = null
    ) =
        updateMessage(
            messageId,
            message,
            contentType = ContentType.Html,
            updateType = updateType,
            disablePreview = disablePreview,
            subCommands = subCommands,
            handler = handler
        )


    fun enterCommand(command: BaseCommand)

    fun leaveCommand(command: BaseCommand? = null)

    fun clearCommands()

    fun <T : Service> getService(clazz: Class<T>): T
}

interface CommandExecutor {
    fun executeCommand(handler: Class<out BaseCommand>, text: String): Boolean

    fun executeCallback(handler: Class<out BaseCommand>, messageId: Int, query: String): Boolean
}

interface UserContext {
    fun userId(): Int

    fun isAdmin(): Boolean
}

interface UserStateService : Service {
    fun getState(key: String): Any?

    fun setState(key: String, value: Any?): Any?

    fun clearState(key: String)
}

interface UserStateServiceFactory : Service {
    fun get(userId: Int): UserStateService
}

enum class ContentType {
    Simple,

    Markdown,

    Html
}

enum class UpdateType {
    UpdateLastPage,

    NewPage
}

data class SubCommand(
    val titleId: String,
    val handler: Class<out BaseCommand>? = null,
    val state: State? = null
)

interface MessageSender {
    /**
     * Sends message to telegram chat
     */
    fun sendMessage(
        chatId: String,
        message: String,
        contentType: ContentType,
        disablePreview: Boolean = true,
        preSendHandler: Consumer<SendMessage> = Consumer { }
    ): Int

    fun updateMessage(
        chatId: String,
        messageId: Int,
        message: String,
        contentType: ContentType,
        disablePreview: Boolean = true,
        preSendHandler: Consumer<EditMessageText> = Consumer { }
    )
}

interface ServiceProvider {
    fun <T : Service> getService(clazz: Class<T>): T
}

interface State<T> {
    /**
     * Returns stored value of type T
     */
    fun get(): T?

    /**
     * Sets value and returns previous value
     */
    fun set(value: T?): T?

    /**
     * Return {@code true} if there is a value present, otherwise {@code false}.
     */
    fun isPresent(): Boolean
}

interface Service

enum class MessageType {
    TEXT,

    CALLBACK
}

data class InputMessage(
    val type: MessageType,
    val query: String,
    val chatId: Long,
    val userId: Int,
    val isAdmin: Boolean,
    val messageId: Int? = null
) {
    init {
        check(userId != 0) { "UserId cannot be $userId" }
    }
}

/**
 * For internal uses
 */
interface CommandInterceptor {
    fun executed(command: BaseCommand, messageType: MessageType)
}

internal object CommandInterceptorEmpty : CommandInterceptor {
    override fun executed(command: BaseCommand, messageType: MessageType) {
    }
}

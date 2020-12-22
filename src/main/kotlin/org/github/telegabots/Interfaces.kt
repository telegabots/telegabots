package org.github.telegabots

import org.github.telegabots.state.State
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import java.util.function.Consumer
import java.util.regex.Pattern

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
    ) : Int

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
    ) : Int

    fun sendHtmlMessage(
        message: String,
        messageType: MessageType = MessageType.Text,
        disablePreview: Boolean = true,
        subCommands: List<List<SubCommand>> = emptyList(),
        handler: Class<out BaseCommand>? = null
    ): Int =
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
    Plain,

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
) {
    companion object {
        inline fun <reified T : BaseCommand> of(state: State? = null, titleId: String = "") =
            of(T::class.java, state, titleId)

        @JvmStatic
        fun of(handler: Class<out BaseCommand>, state: State? = null, titleId: String = ""): SubCommand =
            SubCommand(
                titleId = if (titleId.isNotBlank()) titleId else titleIdOf(handler),
                handler = handler,
                state = state
            )

        @JvmStatic
        fun titleIdOf(handler: Class<out BaseCommand>): String =
            CAMEL_CASE_PAT.matcher(handler.simpleName).replaceAll("$1_$2").toUpperCase()
                .let { if (it.endsWith(PREFIX)) it.substring(0, it.length - PREFIX.length) else it }

        private const val PREFIX = "_COMMAND"
        private val CAMEL_CASE_PAT = Pattern.compile("([a-z\\d])([A-Z]+)")
    }
}

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

    fun <T : Service> tryGetService(clazz: Class<T>): T?
}

/**
 * Specific language related Localization provider
 */
interface LocalizeProvider {
    /**
     * Returns localized string of key itself
     */
    fun getString(key: String): String
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
    Text,

    Callback
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

package org.github.telegabots.api

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import java.io.File
import java.util.function.Consumer
import java.util.regex.Pattern

/**
 * Context used by a command
 */
interface CommandContext : UserContext, CommandExecutor {
    /**
     * Returns current block's id
     *
     * Can be 0 if not associated with a block
     */
    fun blockId(): Long

    /**
     * Returns current page's id.
     *
     * Can be 0 if not associated with a page
     */
    fun pageId(): Long

    fun currentCommand(): BaseCommand

    /**
     * Creates new page into new block
     *
     * Returns created page id
     */
    fun createPage(page: Page): Long

    /**
     * Creates new page into existing (current) block. If block not exists creates new
     *
     * Returns created page id
     */
    fun addPage(page: Page): Long

    /**
     * Updates current page. If page/block not exists creates new
     *
     * Returns updated/created page id
     */
    fun updatePage(page: Page): Long

    fun sendDocument(document: Document)

    fun sendAdminMessage(
        message: String,
        contentType: ContentType,
        disablePreview: Boolean = true
    ): Int

    fun enterCommand(command: BaseCommand)

    fun leaveCommand(command: BaseCommand? = null)

    fun clearCommands()

    fun <T : Service> getService(clazz: Class<T>): T?
}

interface CommandExecutor {
    fun executeTextCommand(handler: Class<out BaseCommand>, text: String): Boolean

    fun executeInlineCommand(handler: Class<out BaseCommand>, messageId: Int, query: String): Boolean
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

data class Page(
    val message: String,
    val contentType: ContentType = ContentType.Plain,
    val messageType: MessageType = MessageType.Text,
    val disablePreview: Boolean = true,
    val subCommands: List<List<SubCommand>> = emptyList(),
    val handler: Class<out BaseCommand>? = null,
    val id: Long = 0L
)

data class Document(
    val file: File,
    val caption: String = "",
    val captionContentType: ContentType = ContentType.Plain,
    val disableNotification: Boolean = false,
    val chatId: String = ""
) {
    companion object {
        @JvmStatic
        fun of(
            file: File,
            caption: String = "",
            captionContentType: ContentType = ContentType.Plain,
            disableNotification: Boolean = false,
            chatId: String = ""
        ) =
            Document(
                file = file,
                caption = caption,
                captionContentType = captionContentType,
                disableNotification = disableNotification,
                chatId = chatId
            )
    }
}

data class StateRef(val items: List<StateItem>) {
    companion object {
        @JvmField
        val Empty = StateRef(emptyList())

        @JvmStatic
        fun of(vararg objs: Any): StateRef = StateRef(objs.map { StateItem(key = StateKey.from(it), value = it) })
    }
}

data class StateItem(
    val key: StateKey,
    val value: Any
) {
    fun equals(type: Class<*>, name: String) = key.equals(type, name)
}

data class StateKey(val type: Class<*>, val name: String) {
    fun equals(type: Class<*>, name: String) = this.type == type && this.name == name

    companion object {
        @JvmStatic
        fun from(obj: Any, name: String = "") = StateKey(type = obj.javaClass, name = name)
    }
}

data class SubCommand(
    val titleId: String,
    val title: String? = null,
    val handler: Class<out BaseCommand>? = null,
    val state: StateRef? = null,
    val behaviour: CommandBehaviour = CommandBehaviour.SeparatePage
) {
    companion object {
        inline fun <reified T : BaseCommand> of(
            state: StateRef? = null,
            titleId: String = "",
            title: String? = null,
            behaviour: CommandBehaviour = CommandBehaviour.SeparatePage
        ) =
            of(T::class.java, state, titleId, title, behaviour)

        @JvmStatic
        fun of(
            titleId: String,
            title: String? = null,
            state: StateRef? = null,
            behaviour: CommandBehaviour = CommandBehaviour.SeparatePage
        ): SubCommand = SubCommand(
            titleId = titleId,
            title = title,
            behaviour = behaviour,
            state = state
        )

        @JvmStatic
        fun of(
            handler: Class<out BaseCommand>,
            state: StateRef? = null,
            titleId: String = "",
            title: String? = null,
            behaviour: CommandBehaviour = CommandBehaviour.SeparatePage
        ): SubCommand = SubCommand(
            titleId = if (titleId.isNotBlank()) titleId else titleIdOf(handler),
            handler = handler,
            state = state,
            title = title,
            behaviour = behaviour
        )

        @JvmStatic
        fun titleIdOf(handler: Class<out BaseCommand>): String =
            CAMEL_CASE_PAT.matcher(handler.simpleName).replaceAll("$1_$2").toUpperCase()
                .let { if (it.endsWith(PREFIX)) it.substring(0, it.length - PREFIX.length) else it }

        private const val PREFIX = "_COMMAND"
        private val CAMEL_CASE_PAT = Pattern.compile("([a-z\\d])([A-Z]+)")
    }
}

/**
 * Message sender of Telegram
 */
interface MessageSender {
    /**
     * Sends new message
     */
    fun sendMessage(
        chatId: String,
        message: String,
        contentType: ContentType = ContentType.Plain,
        disablePreview: Boolean = false,
        preSendHandler: Consumer<SendMessage> = Consumer { }
    ): Int

    /**
     * Updates existing message
     */
    fun updateMessage(
        chatId: String,
        messageId: Int,
        message: String,
        contentType: ContentType = ContentType.Plain,
        disablePreview: Boolean = false,
        preSendHandler: Consumer<EditMessageText> = Consumer { }
    )

    /**
     * Sends file to the chat
     */
    fun sendDocument(
        chatId: String,
        file: File,
        caption: String,
        captionContentType: ContentType = ContentType.Plain,
        disableNotification: Boolean = false
    )

    /**
     * Sends video to the chat
     */
    fun sendVideo(
        chatId: String,
        fileId: String,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    )

    /**
     * Sends video to the chat
     */
    fun sendVideo(
        chatId: String,
        file: File,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    )

    /**
     * Sends images to the chat
     */
    fun sendImages(
        chatId: String,
        files: Array<String>,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    )

    /**
     * Sends images to the chat
     */
    fun sendImages(
        chatId: String,
        files: List<File>,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    )

    /**
     * Sends image to the chat
     */
    fun sendImage(
        chatId: String,
        file: File,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    )
}

interface ServiceProvider {
    fun <T : Service> getService(clazz: Class<T>): T?
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

    Inline
}

enum class CommandBehaviour {
    /**
     * Create separate page and separate state for command
     */
    SeparatePage,

    /**
     * Use parent page without parent local state
     */
    ParentPage,

    /**
     * Use parent page and parent local state
     */
    ParentPageState,
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
        check(chatId != 0L) { "ChatId cannot be $chatId" }
    }

    fun toInputRefresh() = this.copy(query = SystemCommands.REFRESH)
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

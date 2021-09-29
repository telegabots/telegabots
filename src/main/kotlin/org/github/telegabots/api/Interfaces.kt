package org.github.telegabots.api

import org.github.telegabots.context.TaskContextSupport
import org.github.telegabots.task.DefaultTaskIdGenerator
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import java.io.File
import java.time.LocalDateTime
import java.util.function.Consumer
import java.util.regex.Pattern

/**
 * Base context used by a command or task
 */
interface BaseContext : CommandExecutor {
    /**
     * Message id related with current block
     */
    fun messageId(): Int

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

    /**
     * Creates new page into new block
     *
     * Returns created page id
     */
    fun createPage(page: Page): Long

    /**
     * Creates new page into existing (current) or specified block
     *
     * Returns created page id or null if specified block not found
     */
    fun addPage(page: Page): Long?

    /**
     * Updates current page. If page/block not exists creates new
     *
     * Returns updated/created page id or null if page already removed
     */
    fun updatePage(page: Page): Long?

    /**
     * Refresh content of current page or by specified pageId
     *
     * Returns refreshed page id or null if page already removed
     */
    fun refreshPage(pageId: Long = 0, state: StateRef? = null): Long?

    /**
     * Removes page by page id
     *
     * If page is last message and related block will be deleted
     *
     * Returns deleted page id or null if page already removed
     */
    fun deletePage(pageId: Long): Long?

    /**
     * Removes all pages by blockId and related message
     *
     * Returns deleted block id or null if block already removed
     */
    fun deleteBlock(blockId: Long): Long?

    /**
     * Deletes message and related block by messageId
     */
    fun deleteMessage(messageId: Int)

    /**
     * Page is visible and can be updated/refreshed
     */
    fun pageVisible(pageId: Long = 0): Boolean

    /**
     * Returns true if page not removed yet
     *
     * Page can exists but not visible
     */
    fun pageExists(pageId: Long = 0): Boolean

    /**
     * Returns true if page not removed yet
     */
    fun blockExists(blockId: Long = 0): Boolean

    /**
     * Returns list of block. Default chunk size - 10
     *
     * @param lastIndexFrom index from the end
     */
    fun getLastBlocks(lastIndexFrom: Int = 0): List<BlockInfo>

    /**
     * Returns last block
     */
    fun getLastBlock(): BlockInfo?

    /**
     * Returns all pages of specified block
     */
    fun getBlockPages(blockId: Long = 0): List<PageInfo>

    /**
     * Returns page related state
     */
    fun getPageState(pageId: Long = 0): PageStateInfo

    /**
     * Returns state related with block
     */
    fun getBlockState(blockId: Long = 0): BlockStateInfo

    /**
     * Sends document to the current or specified chat
     */
    fun sendDocument(document: Document)

    /**
     * Sends message to admin chat
     */
    fun sendAdminMessage(
        message: String,
        contentType: ContentType,
        disablePreview: Boolean = false
    ): Int

    /**
     * Sends message to the chat
     */
    fun sendMessage(
        message: String,
        contentType: ContentType = ContentType.Plain,
        disablePreview: Boolean = false,
        chatId: String = ""
    ): Int

    fun sendHtmlMessage(
        message: String,
        disablePreview: Boolean = false,
        chatId: String = ""
    ): Int = sendMessage(message, ContentType.Html, disablePreview, chatId)

    fun sendMarkdownMessage(
        message: String,
        disablePreview: Boolean = false,
        chatId: String = ""
    ): Int = sendMessage(message, ContentType.Markdown, disablePreview, chatId)

    // TODO: probably remove
    fun enterCommand(command: BaseCommand)

    // TODO: probably remove
    fun leaveCommand(command: BaseCommand? = null)

    // TODO: probably remove
    fun clearCommands()

    /**
     * Returns task manager
     */
    fun getTaskManager(): TaskManager

    fun <T : Service> getService(clazz: Class<T>): T?

    fun <T : UserService> getUserService(clazz: Class<T>): T?
}

/**
 * Context used by a command
 */
interface CommandContext : BaseContext, UserContext {
    /**
     * Message from user
     */
    fun inputMessage(): InputMessage

    /**
     * Message id related with input user message
     */
    fun inputMessageId(): Int

    /**
     * Returns current command in which input handled
     */
    fun currentCommand(): BaseCommand
}

/**
 * Context used by a task
 */
interface TaskContext : BaseContext {
}

interface CommandExecutor {
    fun executeTextCommand(handler: Class<out BaseCommand>, text: String): Boolean

    fun executeInlineCommand(handler: Class<out BaseCommand>, query: String): Boolean
}

/**
 * Info about block
 */
data class BlockInfo(
    val id: Long,
    val createdAt: LocalDateTime
)

/**
 * Info about page
 */
data class PageInfo(
    val id: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class PageStateInfo(
    val pageId: Long,
    val states: List<StateItem>
)

data class BlockStateInfo(
    val blockId: Long,
    val states: List<StateItem>
)

interface AlertService : Service {
    fun sendHtmlMessage(message: String, disablePreview: Boolean = false)

    fun sendMarkdownMessage(message: String, disablePreview: Boolean = false)
}

/**
 * Base class for task - long live operation subclassed by user
 */
abstract class BaseTask {
    protected val context: TaskContext = TaskContextSupport

    /**
     *  Identifier of the task
     *
     *  Can be used in search
     */
    open fun id(): String = DefaultTaskIdGenerator.getId(this)

    /**
     * Estimate time when task will finished
     *
     * Not null if can be calculated
     */
    open fun estimateEndTime(): LocalDateTime? = null

    /**
     * Human-friendly task description
     */
    abstract fun title(): String

    /**
     * Request to stop task
     */
    abstract fun stopAsync()

    /**
     * Progress percentage of the task. Value from 0 to 100
     */
    open fun progress(): Int? = null

    /**
     * Short description of the current operation
     */
    open fun status(): String? = null

    /**
     * Routine method
     */
    abstract fun run()
}

interface TaskManager : Service {
    /**
     * Register new task
     */
    fun register(task: BaseTask): Task

    /**
     * Remove task from executing. If task is running, it will be stopped before
     */
    fun unregister(task: Task)

    /**
     * Returns all registered tasks
     */
    fun getAll(): List<Task>
}

interface Task {
    /**
     *  Identifier of the task
     *
     *  Can be used in search
     */
    fun id(): String

    /**
     * Human-friendly task description
     */
    fun title(): String

    /**
     * Returns state of the task
     */
    fun state(): TaskState

    /**
     * Runs task if one not started yet
     */
    fun start(onComplete: Consumer<TaskRunResult>? = null)

    /**
     * Async stop task
     */
    fun stop()

    /**
     * Short description of the current operation
     */
    fun status(): String?

    /**
     * Time when task was actually started
     */
    fun startedTime(): LocalDateTime?

    /**
     * Estimate time when task will finished
     *
     * Not null if can be calculated
     */
    fun estimateEndTime(): LocalDateTime?

    /**
     * Progress percentage of the task. Value from 0 to 100
     */
    fun progress(): Int?
}

data class TaskRunResult(
    val task: BaseTask,
    val startedTime: LocalDateTime,
    val runningTime: Long,
    val result: Any? = null,
    val error: Exception? = null
)

enum class TaskState {
    /**
     * Task registered but not started yet
     */
    Initted,

    /**
     * Task is starting
     */
    Starting,

    /**
     * Task finally started
     */
    Started,

    /**
     * Task is stopping
     */
    Stopping,

    /**
     * Task stopped finally
     */
    Stopped
}

interface UserContext {
    fun isAdmin(): Boolean

    fun user(): InputUser
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
    val disablePreview: Boolean = false,
    val subCommands: List<List<SubCommand>> = emptyList(),
    val handler: Class<out BaseCommand>? = null,
    val id: Long = 0L,
    val blockId: Long = 0L,
    val state: StateRef? = null
) {
    override fun toString(): String {
        return "Page(contentType=$contentType, messageType=$messageType, disablePreview=$disablePreview, subCommands=$subCommands, handler=$handler, id=$id,\nmessage='${
            message.take(
                TO_STR_MESSAGE_LEN
            )
        }')"
    }

    companion object {
        private const val TO_STR_MESSAGE_LEN = 25
    }
}

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

/**
 * State used while command executing
 */
data class StateRef(val items: List<StateItem>) {
    override fun toString(): String {
        return when {
            items.isEmpty() -> "StateRef()"
            else -> "StateRef(items=\n${items.joinToString("\n")}})"
        }
    }

    companion object {
        /**
         * Empty state
         */
        @JvmField
        val Empty = StateRef(emptyList())

        /**
         * Creates state from list of objects
         */
        @JvmStatic
        fun of(vararg objs: Any): StateRef = StateRef(objs.map { StateItem(key = StateKey.from(it), value = it) })

        /**
         * Creates state from list of name+object
         */
        @JvmStatic
        fun of(vararg objs: Pair<String, Any>): StateRef =
            StateRef(objs.map { StateItem(key = StateKey.from(it.second, it.first), value = it.second) })
    }
}

/**
 * Single state element
 */
data class StateItem(val key: StateKey, val value: Any) {
    fun equals(type: Class<*>, name: String) = key.equals(type, name)
}

/**
 * State key considered as type and name (optional, can be empty)
 */
data class StateKey(val type: Class<*>, val name: String) {
    fun equals(type: Class<*>, name: String) = this.type == type && this.name == name

    override fun toString(): String {
        return if (name.isNotBlank()) "StateKey(${type.name}['$name'])" else "StateKey(${type.name})"
    }

    companion object {
        @JvmStatic
        fun from(obj: Any, name: String = "") = StateKey(type = obj.javaClass, name = name)
    }
}

/**
 * Data related with single button
 */
data class SubCommand(
    val titleId: String,
    val title: String? = null,
    val handler: Class<out BaseCommand>? = null,
    val behaviour: CommandBehaviour = CommandBehaviour.SeparatePage,
    val state: StateRef? = null
) {
    fun isSystemCommand() = this == REFRESH || this == GO_BACK

    /**
     * Creates new SubCommand with new state
     */
    fun withState(state: StateRef?): SubCommand =
        this.copy(state = state)

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

        val REFRESH = SubCommand.of(SystemCommands.REFRESH)
        val GO_BACK = SubCommand.of(SystemCommands.GO_BACK)
        val NOTHING = SubCommand.of(SystemCommands.NOTHING)
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

    fun deleteMessage(chatId: String, messageId: Int)
}

interface ServiceProvider {
    fun <T : Service> getService(clazz: Class<T>): T?

    fun <T : UserService> getUserService(clazz: Class<T>, user: InputUser): T?
}

/**
 * Factory of getting LocalizeProvider by specified user
 */
interface UserLocalizationFactory : Service {
    /**
     * Returns LocalizeProvider for user by id
     */
    fun getProvider(userId: Long): LocalizeProvider
}

/**
 * Specific language related Localization provider
 */
interface LocalizeProvider {
    /**
     * Language code
     */
    fun language(): String

    /**
     * Returns localized string of key itself
     */
    fun getString(key: String): String
}

/**
 * State manage controller
 */
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

/**
 * Marker of service
 */
interface Service

/**
 * User related service
 */
interface UserService : Service {
    fun userId(): Int
}

/**
 * Message type
 */
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
    val userId: Long,
    val isAdmin: Boolean,
    val user: InputUser,
    val messageId: Int,
    val inlineMessageId: Int?
) {
    init {
        check(userId != 0L) { "UserId cannot be $userId" }
        check(chatId != 0L) { "ChatId cannot be $chatId" }
    }

    fun toInputRefresh() = this.copy(query = SystemCommands.REFRESH)
}

data class InputUser(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val userName: String,
    val isBot: Boolean
)

/**
 * For internal uses
 */
interface CommandInterceptor {
    fun executed(command: BaseCommand, messageType: MessageType)

    companion object {
        @JvmStatic
        public val Empty: CommandInterceptor = CommandInterceptorEmpty
    }
}

internal object CommandInterceptorEmpty : CommandInterceptor {
    override fun executed(command: BaseCommand, messageType: MessageType) {
    }
}

/**
 * Command class validator
 */
interface CommandValidator : Service {
    /**
     * Validates specified command classes
     */
    fun validate(vararg classes: Class<out BaseCommand>)

    /**
     * Finds all command classes by packagePrefix and validates them
     */
    fun validateAll(packagePrefix: String)
}

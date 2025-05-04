package org.github.telegabots.api

import org.github.telegabots.MessageFile
import java.io.File

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
     * Returns current page's id
     *
     * Can be 0 if not associated with a page
     */
    fun pageId(): Long

    /**
     * Message type of current block
     */
    fun messageType(): MessageType

    /**
     * Create specified [BaseCommand] and send [SystemCommands.REFRESH] to it
     */
    fun create(clazz: Class<out BaseCommand>, messageType: MessageType?): Boolean

    /**
     * Create specified [BaseCommand] and send [SystemCommands.REFRESH] to it
     */
    fun create(clazz: Class<out BaseCommand>): Boolean = create(clazz, null)

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
     * Sends image to the chat
     */
    fun sendImage(
        file: MessageFile,
        caption: String,
        captionContentType: ContentType = ContentType.Plain,
        disableNotification: Boolean = false
    ): Int

    /**
     * Updates image in the chat
     */
    fun updateImage(
        messageId: Int,
        file: MessageFile,
        caption: String,
        captionContentType: ContentType = ContentType.Plain
    )

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

    /**
     * Sends message to the chat
     */
    fun sendMessage(
        message: String,
        disablePreview: Boolean = false,
    ): Int = sendMessage(message, ContentType.Plain, disablePreview, "")

    /**
     * Sends message to the chat
     */
    fun sendMessage(message: String): Int = sendMessage(message, ContentType.Plain, false, "")

    /**
     * Sends message to the chat as html
     */
    fun sendHtmlMessage(
        message: String,
        disablePreview: Boolean = false,
        chatId: String = ""
    ): Int = sendMessage(message, ContentType.Html, disablePreview, chatId)

    /**
     * Sends message to the chat as html
     */
    fun sendHtmlMessage(
        message: String,
        disablePreview: Boolean = false
    ): Int = sendMessage(message, ContentType.Html, disablePreview, "")

    /**
     * Sends message to the chat as html
     */
    fun sendHtmlMessage(message: String): Int = sendMessage(message, ContentType.Html, false, "")

    /**
     * Sends message to the chat as markdown
     */
    fun sendMarkdownMessage(
        message: String,
        disablePreview: Boolean = false,
        chatId: String = ""
    ): Int = sendMessage(message, ContentType.Markdown, disablePreview, chatId)

    /**
     * Sends message to the chat as markdown
     */
    fun sendMarkdownMessage(
        message: String,
        disablePreview: Boolean = false,
    ): Int = sendMessage(message, ContentType.Markdown, disablePreview, "")

    /**
     * Sends message to the chat as markdown
     */
    fun sendMarkdownMessage(message: String): Int = sendMessage(message, ContentType.Markdown, false, "")

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

    fun <T : Service> getService(clazz: Class<T>): T

    fun <T : Service> tryGetService(clazz: Class<T>): T?

    fun <T : UserService> getUserService(clazz: Class<T>): T

    fun <T : UserService> tryGetUserService(clazz: Class<T>): T?

    // === Builder methods ===

    /**
     * Creates new [PageBuilder].
     */
    fun page(message: String): PageBuilder

    fun page(file: MessageFile): PageBuilder

    fun page(file: File): PageBuilder = page(MessageFile.from(file))
}

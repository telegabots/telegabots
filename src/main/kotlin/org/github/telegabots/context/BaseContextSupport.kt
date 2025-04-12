package org.github.telegabots.context

import org.github.telegabots.MessageFile
import org.github.telegabots.api.*

/**
 * Base methods of Context for current executing command or task
 */
abstract class BaseContextSupport<T : BaseContext> : BaseContext {
    private val contextCurrent = ThreadLocal<T>()

    internal fun setContext(context: T?) =
        if (context != null)
            contextCurrent.set(context)
        else
            contextCurrent.remove()

    protected open fun current(): T =
        contextCurrent.get() ?: throw IllegalStateException("Context not initialized")

    override fun messageId(): Int = current().messageId()

    override fun blockId(): Long = current().blockId()

    override fun pageId(): Long = current().pageId()

    override fun messageType(): MessageType  = current().messageType()

    override fun createPage(page: Page): Long = current().createPage(page)

    override fun addPage(page: Page): Long? = current().addPage(page)

    override fun updatePage(page: Page): Long? = current().updatePage(page)

    override fun refreshPage(pageId: Long, state: StateRef?) = current().refreshPage(pageId, state)

    override fun deletePage(pageId: Long) = current().deletePage(pageId)

    override fun deleteBlock(blockId: Long) = current().deleteBlock(blockId)

    override fun deleteMessage(messageId: Int) = current().deleteMessage(messageId)

    override fun pageVisible(pageId: Long) = current().pageVisible(pageId)

    override fun pageExists(pageId: Long): Boolean = current().pageExists(pageId)

    override fun blockExists(blockId: Long): Boolean = current().blockExists(blockId)

    override fun getLastBlocks(lastIndexFrom: Int): List<BlockInfo> = current().getLastBlocks(lastIndexFrom)

    override fun getLastBlock(): BlockInfo? = current().getLastBlock()

    override fun getBlockPages(blockId: Long): List<PageInfo> = current().getBlockPages(blockId)

    override fun getPageState(pageId: Long): PageStateInfo = current().getPageState(pageId)

    override fun getBlockState(blockId: Long): BlockStateInfo = current().getBlockState(blockId)

    override fun sendDocument(document: Document) = current().sendDocument(document)

    override fun sendImage(
        file: MessageFile,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    ): Int = current().sendImage(file, caption, captionContentType, disableNotification)

    override fun updateImage(
        messageId: Int,
        file: MessageFile,
        caption: String,
        captionContentType: ContentType
    ) = current().updateImage(messageId, file, caption, captionContentType)

    override fun sendAdminMessage(message: String, contentType: ContentType, disablePreview: Boolean) =
        current().sendAdminMessage(message, contentType, disablePreview)

    override fun sendMessage(message: String, contentType: ContentType, disablePreview: Boolean, chatId: String): Int =
        current().sendMessage(message, contentType, disablePreview, chatId)

    override fun enterCommand(command: BaseCommand) = current().enterCommand(command)

    override fun leaveCommand(command: BaseCommand?) = current().leaveCommand(command)

    override fun clearCommands() = current().clearCommands()

    override fun getTaskManager(): TaskManager = current().getTaskManager()

    override fun <T : Service> getService(clazz: Class<T>): T = current().getService(clazz)

    override fun <T : Service> tryGetService(clazz: Class<T>): T? = current().tryGetService(clazz)

    override fun <T : UserService> getUserService(clazz: Class<T>): T =
        current().getUserService(clazz)

    override fun <T : UserService> tryGetUserService(clazz: Class<T>): T? =
        current().tryGetUserService(clazz)

    override fun executeTextCommand(handler: Class<out BaseCommand>, text: String): Boolean =
        current().executeTextCommand(handler, text)

    override fun executeInlineCommand(handler: Class<out BaseCommand>, query: String): Boolean =
        current().executeInlineCommand(handler, query)

    override fun page(message: String): PageBuilder = current().page(message)

    override fun page(file: MessageFile): PageBuilder = current().page(file)
}

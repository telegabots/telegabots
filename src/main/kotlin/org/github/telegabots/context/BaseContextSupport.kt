package org.github.telegabots.context

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.BaseContext
import org.github.telegabots.api.BlockInfo
import org.github.telegabots.api.BlockStateInfo
import org.github.telegabots.api.ContentType
import org.github.telegabots.api.Document
import org.github.telegabots.api.Page
import org.github.telegabots.api.PageInfo
import org.github.telegabots.api.PageStateInfo
import org.github.telegabots.api.Service
import org.github.telegabots.api.StateRef
import org.github.telegabots.api.TaskManager
import org.github.telegabots.api.UserService

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

    override fun createPage(page: Page): Long = current().createPage(page)

    override fun addPage(page: Page): Long = current().addPage(page)

    override fun updatePage(page: Page): Long? = current().updatePage(page)

    override fun refreshPage(pageId: Long, state: StateRef?) = current().refreshPage(pageId, state)

    override fun deletePage(pageId: Long) = current().deletePage(pageId)

    override fun deleteBlock(blockId: Long) = current().deleteBlock(blockId)

    override fun deleteMessage(messageId: Int) = current().deleteMessage(messageId)

    override fun canUpdate(pageId: Long) = current().canUpdate(pageId)

    override fun pageExists(pageId: Long): Boolean = current().pageExists(pageId)

    override fun blockExists(blockId: Long): Boolean = current().blockExists(blockId)

    override fun getLastBlocks(lastIndexFrom: Int): List<BlockInfo> = current().getLastBlocks(lastIndexFrom)

    override fun getLastBlock(): BlockInfo? = current().getLastBlock()

    override fun getBlockPages(blockId: Long): List<PageInfo> = current().getBlockPages(blockId)

    override fun getPageState(pageId: Long): PageStateInfo = current().getPageState(pageId)

    override fun getBlockState(blockId: Long): BlockStateInfo = current().getBlockState(blockId)

    override fun sendDocument(document: Document) = current().sendDocument(document)

    override fun sendAdminMessage(message: String, contentType: ContentType, disablePreview: Boolean) =
        current().sendAdminMessage(message, contentType, disablePreview)

    override fun sendMessage(message: String, contentType: ContentType, disablePreview: Boolean, chatId: String): Int =
        current().sendMessage(message, contentType, disablePreview, chatId)

    override fun enterCommand(command: BaseCommand) = current().enterCommand(command)

    override fun leaveCommand(command: BaseCommand?) = current().leaveCommand(command)

    override fun clearCommands() = current().clearCommands()

    override fun getTaskManager(): TaskManager = current().getTaskManager()

    override fun <T : Service> getService(clazz: Class<T>): T? = current().getService(clazz)

    override fun <T : UserService> getUserService(clazz: Class<T>): T? =
        current().getUserService(clazz)

    override fun executeTextCommand(handler: Class<out BaseCommand>, text: String): Boolean =
        current().executeTextCommand(handler, text)

    override fun executeInlineCommand(handler: Class<out BaseCommand>, query: String): Boolean =
        current().executeInlineCommand(handler, query)
}

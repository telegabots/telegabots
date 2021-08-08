package org.github.telegabots.task

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.ContentType
import org.github.telegabots.api.Document
import org.github.telegabots.api.InputUser
import org.github.telegabots.api.Page
import org.github.telegabots.api.Service
import org.github.telegabots.api.ServiceProvider
import org.github.telegabots.api.StateRef
import org.github.telegabots.api.TaskContext
import org.github.telegabots.api.TaskManager
import org.github.telegabots.api.UserService

/**
 * TODO: use instead CommandContextImpl
 */
class TaskContextImpl(
    private val blockId: Long,
    private val pageId: Long,
    private val user: InputUser,
    private val serviceProvider: ServiceProvider
) : TaskContext {
    override fun messageId(): Int {
        TODO("Not yet implemented")
    }

    override fun blockId(): Long {
        TODO("Not yet implemented")
    }

    override fun pageId(): Long {
        TODO("Not yet implemented")
    }

    override fun createPage(page: Page): Long {
        TODO("Not yet implemented")
    }

    override fun addPage(page: Page): Long {
        TODO("Not yet implemented")
    }

    override fun updatePage(page: Page): Long {
        TODO("Not yet implemented")
    }

    override fun refreshPage(pageId: Long, state: StateRef?) {
        TODO("Not yet implemented")
    }

    override fun deletePage(pageId: Long) {
        TODO("Not yet implemented")
    }

    override fun deleteBlock(blockId: Long) {
        TODO("Not yet implemented")
    }

    override fun deleteMessage(messageId: Int) {
        TODO("Not yet implemented")
    }

    override fun sendDocument(document: Document) {
        TODO("Not yet implemented")
    }

    override fun sendAdminMessage(message: String, contentType: ContentType, disablePreview: Boolean): Int {
        TODO("Not yet implemented")
    }

    override fun sendMessage(message: String, contentType: ContentType, disablePreview: Boolean, chatId: String): Int {
        TODO("Not yet implemented")
    }

    override fun enterCommand(command: BaseCommand) {
        TODO("Not yet implemented")
    }

    override fun leaveCommand(command: BaseCommand?) {
        TODO("Not yet implemented")
    }

    override fun clearCommands() {
        TODO("Not yet implemented")
    }

    override fun getTaskManager(): TaskManager {
        TODO("Not yet implemented")
    }

    override fun <T : Service> getService(clazz: Class<T>): T? {
        TODO("Not yet implemented")
    }

    override fun <T : UserService> getUserService(clazz: Class<T>): T? {
        TODO("Not yet implemented")
    }

    override fun executeTextCommand(handler: Class<out BaseCommand>, text: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun executeInlineCommand(handler: Class<out BaseCommand>, query: String): Boolean {
        TODO("Not yet implemented")
    }

}

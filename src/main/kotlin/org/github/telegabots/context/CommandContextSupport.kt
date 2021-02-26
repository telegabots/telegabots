package org.github.telegabots.context

import org.github.telegabots.api.*

/**
 * Supports CommandContext for current executing command
 */
object CommandContextSupport : CommandContext {
    private val contextCurrent = ThreadLocal<CommandContext>()

    internal fun setContext(context: CommandContext?) =
        if (context != null) contextCurrent.set(context) else contextCurrent.remove()

    private inline fun current(): CommandContext {
        return contextCurrent.get()
            ?: throw IllegalStateException("Command context not initialized for current command")
    }

    override fun inlineMessageId(): Int? = current().inlineMessageId()

    override fun blockId(): Long = current().blockId()

    override fun pageId(): Long = current().pageId()

    override fun currentCommand(): BaseCommand = current().currentCommand()

    override fun createPage(page: Page): Long = current().createPage(page)

    override fun addPage(page: Page): Long = current().addPage(page)

    override fun updatePage(page: Page): Long = current().updatePage(page)

    override fun sendDocument(document: Document) = current().sendDocument(document)

    override fun sendAdminMessage(message: String, contentType: ContentType, disablePreview: Boolean) =
        current().sendAdminMessage(message, contentType, disablePreview)

    override fun sendMessage(message: String, contentType: ContentType, disablePreview: Boolean, chatId: String): Int =
        current().sendMessage(message, contentType, disablePreview, chatId)

    override fun enterCommand(command: BaseCommand) = current().enterCommand(command)

    override fun leaveCommand(command: BaseCommand?) = current().leaveCommand(command)

    override fun clearCommands() = current().clearCommands()

    override fun <T : Service> getService(clazz: Class<T>): T? = current().getService(clazz)

    override fun <T : UserService> getUserService(clazz: Class<T>, userId: Int): T?  = current().getUserService(clazz, userId)

    override fun userId(): Int = current().userId()

    override fun isAdmin(): Boolean = current().isAdmin()

    override fun executeTextCommand(handler: Class<out BaseCommand>, text: String): Boolean =
        current().executeTextCommand(handler, text)

    override fun executeInlineCommand(handler: Class<out BaseCommand>, query: String): Boolean =
        current().executeInlineCommand(handler, query)
}

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

    override fun currentCommand(): BaseCommand = current().currentCommand()

    override fun createPage(page: Page): Long = current().createPage(page)

    override fun addPage(page: Page): Long = current().addPage(page)

    override fun updatePage(page: Page): Long = current().updatePage(page)

    override fun sendAdminMessage(message: String, contentType: ContentType, disablePreview: Boolean) =
        current().sendAdminMessage(message, contentType, disablePreview)

    override fun enterCommand(command: BaseCommand) = current().enterCommand(command)

    override fun leaveCommand(command: BaseCommand?) = current().leaveCommand(command)

    override fun clearCommands() = current().clearCommands()

    override fun <T : Service> getService(clazz: Class<T>): T = current().getService(clazz)

    override fun userId(): Int = current().userId()

    override fun isAdmin(): Boolean = current().isAdmin()

    override fun executeCommand(handler: Class<out BaseCommand>, text: String): Boolean =
        current().executeCommand(handler, text)

    override fun executeCallback(handler: Class<out BaseCommand>, messageId: Int, query: String): Boolean =
        current().executeCallback(handler, messageId, query)
}

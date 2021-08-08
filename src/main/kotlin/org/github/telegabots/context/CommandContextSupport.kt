package org.github.telegabots.context

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.CommandContext
import org.github.telegabots.api.InputMessage
import org.github.telegabots.api.InputUser

/**
 * Supports CommandContext for current executing command
 */
object CommandContextSupport : BaseContextSupport<CommandContext>(), CommandContext {
    override fun inputMessage(): InputMessage = current().inputMessage()

    override fun inputMessageId(): Int = current().inputMessageId()

    override fun currentCommand(): BaseCommand = current().currentCommand()

    override fun isAdmin(): Boolean = current().isAdmin()

    override fun user(): InputUser = current().user()
}

package org.github.telegabots.service

import org.github.telegabots.api.*
import org.github.telegabots.context.BaseContextSupport

/**
 * [CommandContext] for [SystemCommands.NOTHING] or when input should be ignored
 */
object NothingCommandCallContext : CommandContext, BaseContextSupport<CommandContext>() {
    override fun execute(): Boolean = true

    override fun inputMessage(): InputMessage = notExpected()

    override fun inputMessageId(): Int = notExpected()

    override fun currentCommand(): BaseCommand = notExpected()

    override fun isAdmin(): Boolean = notExpected()

    override fun getUser(): InputUser = notExpected()

    private fun notExpected(): Nothing {
        error("Not expected to be called")
    }
}

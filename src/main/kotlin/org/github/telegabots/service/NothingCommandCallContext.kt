package org.github.telegabots.service

import org.github.telegabots.api.SystemCommands

/**
 * [CommandCallContext] for [SystemCommands.NOTHING] or when input should be ignored
 */
object NothingCommandCallContext : CommandCallContext {
    override fun execute(): Boolean = true
}

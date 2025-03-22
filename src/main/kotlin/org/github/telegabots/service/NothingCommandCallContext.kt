package org.github.telegabots.service

import org.github.telegabots.api.SystemCommands

/**
 * [CommandCallContext] for [SystemCommands.NOTHING]
 */
object NothingCommandCallContext : CommandCallContext {
    override fun execute(): Boolean = true
}

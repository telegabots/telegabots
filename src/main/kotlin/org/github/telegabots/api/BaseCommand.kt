package org.github.telegabots.api

import org.slf4j.LoggerFactory
import org.github.telegabots.context.CommandContextSupport

/**
 * Base command of all commands
 */
abstract class BaseCommand {
    @JvmField
    protected val log = LoggerFactory.getLogger(javaClass)!!
    @JvmField
    protected val context: CommandContext = CommandContextSupport

    /**
     * TODO: remove if not used
     */
    open fun suppressCommonCommands(): Boolean = false

    /**
     * TODO: remove if not used
     */
    open fun suppressGoBackCommand() = false

    /**
     * Return true if this command can be executed only by admin role
     */
    open fun isOnlyForAdmin(): Boolean = false

    override fun toString(): String = javaClass.simpleName
}

fun BaseCommand.isEmpty() = this is EmptyCommand

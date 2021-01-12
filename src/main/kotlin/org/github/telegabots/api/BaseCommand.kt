package org.github.telegabots.api

import org.slf4j.LoggerFactory
import org.github.telegabots.context.CommandContextSupport

abstract class BaseCommand {
    @JvmField
    protected val log = LoggerFactory.getLogger(javaClass)!!
    @JvmField
    protected val context: CommandContext = CommandContextSupport

    open fun suppressCommonCommands(): Boolean = false

    open fun suppressGoBackCommand() = false

    override fun toString(): String = javaClass.simpleName
}

fun BaseCommand.isEmpty() = this is EmptyCommand

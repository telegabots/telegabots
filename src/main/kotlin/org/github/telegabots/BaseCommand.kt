package org.github.telegabots

import org.slf4j.LoggerFactory
import org.github.telegabots.annotation.CallbackHandler
import org.github.telegabots.annotation.CommandHandler

abstract class BaseCommand {
    protected val log = LoggerFactory.getLogger(javaClass)!!

    open fun suppressCommonCommands(): Boolean = false

    open fun suppressGoBackCommand() = false

    override fun toString(): String = javaClass.simpleName
}

class EmptyCommand : BaseCommand() {
    @CommandHandler
    fun execute(text: String): Boolean {
        log.warn("Empty command executed: $text")
        return true
    }

    @CallbackHandler
    fun executeCallback(text: String): Boolean {
        log.warn("Empty command callback executed: $text")
        return true
    }
}

fun BaseCommand.isEmpty() = this is EmptyCommand

class GoBackCommand : BaseCommand()

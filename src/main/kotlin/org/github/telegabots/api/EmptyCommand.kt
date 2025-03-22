package org.github.telegabots.api

import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler

/**
 * Implementation of [BaseCommand] which does nothing
 */
class EmptyCommand : BaseCommand() {
    @TextHandler
    fun execute(text: String): Boolean {
        log.warn("Empty command executed: $text")
        return true
    }

    @InlineHandler
    fun executeInline(text: String) {
        log.warn("Empty command inline executed: $text")
    }

    companion object {
        @JvmField
        val INSTANCE = EmptyCommand()
    }
}

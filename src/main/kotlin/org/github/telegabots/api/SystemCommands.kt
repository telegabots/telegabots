package org.github.telegabots.api

import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler

/**
 * All system commands ids
 */
object SystemCommands {
    /**
     * Removes current page and send _REFRESH to previous one
     */
    const val GO_BACK = "_BACK"

    /**
     * Refresh content of current page
     */
    const val REFRESH = "_REFRESH"
}

class EmptyCommand : BaseCommand() {
    @TextHandler
    fun execute(text: String): Boolean {
        log.warn("Empty command executed: $text")
        return true
    }

    @InlineHandler
    fun executeInline(text: String): Boolean {
        log.warn("Empty command inline executed: $text")
        return true
    }
}

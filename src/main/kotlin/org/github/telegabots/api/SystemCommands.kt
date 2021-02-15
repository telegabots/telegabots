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
    const val GO_BACK = "_GO_BACK"

    /**
     * Refresh content of current page
     */
    const val REFRESH = "_REFRESH"

    val ALL = listOf(GO_BACK, REFRESH)
}

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
}

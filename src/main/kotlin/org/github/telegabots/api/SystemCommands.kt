package org.github.telegabots.api

import org.github.telegabots.api.annotation.CallbackHandler
import org.github.telegabots.api.annotation.CommandHandler

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

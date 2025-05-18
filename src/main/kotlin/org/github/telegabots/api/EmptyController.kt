package org.github.telegabots.api

import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler

/**
 * Implementation of [BaseController] which does nothing
 */
class EmptyController : BaseController() {
    @TextHandler
    fun execute(text: String): Boolean {
        log.warn("Empty controller executed: $text")
        return true
    }

    @InlineHandler
    fun executeInline(text: String) {
        log.warn("Empty controller inline executed: $text")
    }

    companion object {
        @JvmField
        val INSTANCE = EmptyController()
    }
}

package org.examples.testbot.controllers

import org.github.telegabots.api.BaseController
import org.github.telegabots.api.MessageType
import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler

class PrintController : BaseController() {
    @TextHandler
    fun handle(message: String) {
        context.page("TEXT MESSAGE: $message")
            .enableBack()
            .create()
    }

    @InlineHandler
    fun handleInline(message: String) {
        context.page("INLINE MESSAGE: $message")
            .messageType(MessageType.Inline)
            .enableBack()
            .update()
    }
}

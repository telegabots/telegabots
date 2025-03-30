package org.examples.testbot.commands

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.MessageType
import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler

class PrintCommand : BaseCommand() {
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

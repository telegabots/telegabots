package org.examples.allfeaturedbot.commands

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.MessageType
import org.github.telegabots.api.SubCommand
import org.github.telegabots.api.SystemCommands
import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler

class RootCommand : BaseCommand() {
    @TextHandler
    fun handle(message: String) {
        context.page("Hello from bot! You said: $message")
            .messageType(MessageType.Inline)
            .subCommands(SubCommand.of<FileExplorerCommand>())
            .create()
    }

    @InlineHandler
    fun handleInline(message: String) {
        if (message == SystemCommands.REFRESH) {
            context.page("Main menu")
                .messageType(MessageType.Inline)
                .subCommands(SubCommand.of<FileExplorerCommand>())
                .update()
        } else {
            TODO(message)
        }
    }
}

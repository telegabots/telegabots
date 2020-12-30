package org.examples.allfeaturedbot.commands

import org.github.telegabots.api.*
import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler

class RootCommand : BaseCommand() {
    @TextHandler
    fun handle(message: String) {
        context.createPage(
            Page(
                "Hello from bot! You said: $message",
                messageType = MessageType.Inline,
                subCommands = listOf(listOf(SubCommand.of<FileExplorerCommand>()))
            )
        )
    }

    @InlineHandler
    fun handleInline(message: String, messageId: Int) {
        if (message == SystemCommands.REFRESH) {
            context.updatePage(
                Page(
                    "Main menu",
                    messageType = MessageType.Inline,
                    subCommands = listOf(listOf(SubCommand.of<FileExplorerCommand>()))
                )
            )
        } else {
            TODO(message)
        }
    }
}

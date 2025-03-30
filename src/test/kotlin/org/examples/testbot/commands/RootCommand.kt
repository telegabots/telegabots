package org.examples.testbot.commands

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.MessageType
import org.github.telegabots.api.SystemCommands
import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler
import org.github.telegabots.std.cmd.LanguageCommand

/**
 * Root command for bot.
 *
 * Handles first message (/start) from user and shows main menu.
 */
class RootCommand : BaseCommand() {
    @TextHandler
    fun handle(message: String) {
        context.page("Hello from bot! You said: $message")
            .messageType(MessageType.Inline)
            .subCommands(FileExplorerCommand::class.java, LanguageCommand::class.java)
            .create()
    }

    @InlineHandler
    fun handleInline(message: String) {
        if (message == SystemCommands.REFRESH) {
            context.page("Main menu")
                .messageType(MessageType.Inline)
                .subCommands(FileExplorerCommand::class.java, LanguageCommand::class.java)
                .update()
        } else {
            TODO(message)
        }
    }
}

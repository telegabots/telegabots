package org.examples.testbot.commands

import org.github.telegabots.MessageFile
import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.MessageType
import org.github.telegabots.api.SubCommand
import java.io.File

/**
 * Sample command to show file via [MessageType.Photo]
 */
class FileViewCommand : BaseCommand() {
    @InlineHandler
    fun handleInline(message: String) {
        val file = File("")
        // TODO: create image and send it
        context.page(MessageFile.from(file))
            .subCommands(SubCommand.of("1"), SubCommand.of("2"), SubCommand.of("3"), SubCommand.of("clear"))
            .messageType(MessageType.Inline)
            .enableBack()
            .update()
    }
}

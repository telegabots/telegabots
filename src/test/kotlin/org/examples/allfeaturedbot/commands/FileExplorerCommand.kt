package org.examples.allfeaturedbot.commands

import org.github.telegabots.api.*
import org.github.telegabots.api.annotation.InlineHandler
import java.io.File

class FileExplorerCommand : BaseCommand() {
    @InlineHandler
    fun index(message: String, messageId: Int) {
        if (message == SystemCommands.REFRESH) {
            val files = File("/").listFiles()

            context.updatePage(
                Page(
                    "List of file", messageType = MessageType.Inline,
                    subCommands = listOf(files.map { SubCommand.of(it.name) })
                )
            )
        }
    }
}

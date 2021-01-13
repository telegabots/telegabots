package org.examples.allfeaturedbot.commands

import org.github.telegabots.api.*
import org.github.telegabots.api.annotation.InlineHandler
import java.io.File
import java.util.*

class FileExplorerCommand : BaseCommand() {
    @InlineHandler
    fun index(message: String, messageId: Int, currentDir: State<String>) {
        val currentPath = currentDir.get() ?: "/"
        val nextPath = when (message) {
            SystemCommands.REFRESH -> currentPath
            UP_DIR -> File(currentPath).parentFile.absolutePath
            else -> File(currentPath, message).absolutePath
        }

        currentDir.set(nextPath)
        val currentFile = File(nextPath)

        if (currentFile.isDirectory) {
            val files = currentFile.listFiles()
            files.sortBy { it.name }
            val allFiles = files.map { SubCommand.of(it.name, if (it.isDirectory) "[${it.name}]" else it.name) }
                .toMutableList()

            log.debug("currentPath: {}, nextPath: {}, files count: {}", currentPath, nextPath, allFiles.size)

            if (nextPath != "/") {
                allFiles.add(0, SubCommand.of(UP_DIR))
            }

            val subCommands = mutableListOf<List<SubCommand>>()
            val rowSize = 5

            for (i in 0..allFiles.size / rowSize) {
                val index = i * rowSize
                subCommands.add(allFiles.subList(index, Math.min(index + rowSize, allFiles.size)))
            }

            context.updatePage(
                Page(
                    """
                        Files of directory: *$nextPath*
                        Count: ${allFiles.size}
                    """.trimIndent(),
                    contentType = ContentType.Markdown,
                    messageType = MessageType.Inline,
                    subCommands = subCommands
                )
            )
        } else {
            val lastModified = Date(currentFile.lastModified())

            context.updatePage(
                Page(
                    """
                        Information about file: *$nextPath*
                        Size: ${currentFile.length()} bytes
                        Last modified: $lastModified
                    """.trimIndent(),
                    contentType = ContentType.Markdown,
                    messageType = MessageType.Inline,
                    subCommands = listOf(
                        listOf(
                            SubCommand.of(UP_DIR),
                            SubCommand.of<FileDownloadCommand>(
                                title = "Download",
                                state = StateRef.of(nextPath),
                                behaviour = CommandBehaviour.ParentPage
                            )
                        )
                    )
                )
            )
        }
    }

    companion object {
        const val UP_DIR = ".."
    }
}

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
            val allFiles = files.map { SubCommand.of(it.name) }.toMutableList()

            log.debug("currentPath: {}, nextPath: {}, files count: {}", currentPath, nextPath, allFiles.size)

            if (nextPath != "/") {
                allFiles.add(0, SubCommand.of(UP_DIR))
            }

            val subCommands = mutableListOf<List<SubCommand>>()

            for (i in 0..allFiles.size / 5) {
                val index = i * 5
                subCommands.add(allFiles.subList(index, Math.min(index + 5, allFiles.size)))
            }

            context.updatePage(
                Page(
                    "Files of directory: *$nextPath*",
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
                    subCommands = listOf(listOf(SubCommand.of(UP_DIR)))
                )
            )
        }
    }

    companion object {
        const val UP_DIR = ".."
    }
}

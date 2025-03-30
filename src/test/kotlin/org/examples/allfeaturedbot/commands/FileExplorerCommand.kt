package org.examples.allfeaturedbot.commands

import org.apache.commons.io.FileUtils
import org.examples.allfeaturedbot.tasks.CalculateDirSizeProgressInfo
import org.github.telegabots.api.*
import org.github.telegabots.api.annotation.InlineHandler
import java.io.File
import java.util.*

class FileExplorerCommand : BaseCommand() {
    @InlineHandler
    fun index(message: String, currentDir: State<String>, progressInfo: State<CalculateDirSizeProgressInfo>) {
        val currentPath = currentDir.get() ?: "/"
        val nextPath = when (message) {
            SystemCommands.REFRESH -> currentPath
            UP_DIR -> File(currentPath).parentFile.absolutePath
            else -> File(currentPath, message).absolutePath // TODO: security check message from tg client
        }

        currentDir.set(nextPath)
        val currentFile = File(nextPath)

        log.debug("message: {}, currentPath: {}, nextPath: {}", message, currentPath, nextPath)

        if (currentFile.isDirectory) {
            val files = currentFile.listFiles() ?: emptyArray()
            files.sortBy { it.name }
            val allFiles = files.map { SubCommand.of(it.name, if (it.isDirectory) "[${it.name}]" else it.name) }
                .toMutableList()
            val progressInfo = progressInfo.get()
            val progressStatus = progressInfo?.let {

                val st = if (it.status.isNotBlank()) "```\nStatus: ${it.status}\n```" else ""

                """Size: ${FileUtils.byteCountToDisplaySize(it.size)}
                   |Progress: ${it.percent}%
                   |$st""".trimMargin()
            } ?: ""

            if (progressInfo != null) {
                allFiles.add(0, SubCommand.of(SystemCommands.NOTHING, title = "Calculating...${progressInfo.percent}%"))
            } else {
                allFiles.add(
                    0, SubCommand.of<CalculateDirSizeCommand>(
                        state = StateRef.of(nextPath),
                        behaviour = CommandBehaviour.ParentPage,
                        title = "Calculate dir size"
                    )
                )
            }

            if (nextPath != "/") {
                allFiles.add(0, SubCommand.of(UP_DIR))
            }

            val subCommands = mutableListOf<List<SubCommand>>()
            val rowSize = 5

            for (i in 0..allFiles.size / rowSize) {
                val index = i * rowSize
                subCommands.add(allFiles.subList(index, Math.min(index + rowSize, allFiles.size)))
            }
            context.page(
                """
                Files of directory: *$nextPath*
                Count: ${allFiles.size - 1}
                $progressStatus
                """.trimIndent()
            )
                .messageType(MessageType.Inline)
                .contentType(ContentType.Markdown)
                .subCommands(subCommands)
                .enableBack()
                .update()
        } else {
            val lastModified = Date(currentFile.lastModified())

            context.page(
                """
                        Information about file: *$nextPath*
                        Size: ${currentFile.length()} bytes
                        Last modified: $lastModified
                    """.trimIndent()
            )
                .contentType(ContentType.Markdown)
                .messageType(MessageType.Inline)
                .subCommands(
                    SubCommand.of(UP_DIR),
                    SubCommand.of<FileDownloadCommand>(
                        title = "Download",
                        state = StateRef.of(nextPath),
                        behaviour = CommandBehaviour.ParentPage
                    )
                )
                .enableBack()
                .update()
        }
    }

    companion object {
        const val UP_DIR = ".."
    }
}

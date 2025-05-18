package org.examples.testbot.controllers

import org.apache.commons.io.FileUtils
import org.examples.testbot.tasks.CalculateDirSizeProgressInfo
import org.github.telegabots.api.*
import org.github.telegabots.api.annotation.InlineHandler
import java.io.File
import java.util.*

class FileExplorerController : BaseController() {
    @InlineHandler
    fun index(message: String, currentDir: State<String>, progressInfo: State<CalculateDirSizeProgressInfo>) {
        val currentPath = currentDir.get() ?: "/"
        val nextPath = when (message) {
            SystemMessages.REFRESH -> currentPath
            UP_DIR -> File(currentPath).parentFile.absolutePath
            else -> File(currentPath, message).absolutePath // TODO: security check message from tg client
        }

        currentDir.set(nextPath)
        val nextFile = File(nextPath)

        log.debug("message: {}, currentPath: {}, nextPath: {}", message, currentPath, nextPath)

        if (nextFile.isDirectory) {
            handleDirectory(nextFile, progressInfo)
        } else {
            handleFile(nextFile)
        }
    }

    private fun handleDirectory(
        directory: File,
        progressInfo: State<CalculateDirSizeProgressInfo>
    ) {
        val nextPath: String = directory.absolutePath
        val files = directory.listFiles() ?: emptyArray()
        files.sortBy { it.name }
        val allFiles = files.map { Button.of(it.name, if (it.isDirectory) "[${it.name}]" else it.name) }
            .toMutableList()
        val progressInfo = progressInfo.get()
        val progressStatus = progressInfo?.let {

            val st = if (it.status.isNotBlank()) "```\nStatus: ${it.status}\n```" else ""

            """Size: ${FileUtils.byteCountToDisplaySize(it.size)}
                       |Progress: ${it.percent}%
                       |$st""".trimMargin()
        } ?: ""

        if (progressInfo != null) {
            allFiles.add(0, Button.of(SystemMessages.NOTHING, title = "Calculating...${progressInfo.percent}%"))
        } else {
            allFiles.add(
                0, Button.of<CalculateDirSizeController>(
                    state = StateRef.of(nextPath),
                    title = "Calculate dir size"
                )
            )
        }

        if (nextPath != "/") {
            allFiles.add(0, Button.of(UP_DIR))
        }

        val buttons = mutableListOf<List<Button>>()
        val rowSize = 5

        for (i in 0..allFiles.size / rowSize) {
            val index = i * rowSize
            buttons.add(allFiles.subList(index, Math.min(index + rowSize, allFiles.size)))
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
            .buttons(buttons)
            .enableBack()
            .update()
    }

    private fun handleFile(file: File) {
        val lastModified = Date(file.lastModified())
        val nextPath: String = file.absolutePath

        context.page(
            """
            Information about file: *$nextPath*
            Size: ${file.length()} bytes
            Last modified: $lastModified
            """.trimIndent()
        )
            .contentType(ContentType.Markdown)
            .messageType(MessageType.Inline)
            .buttons(
                Button.of(UP_DIR),
                Button.of<FileDownloadController>(
                    title = "Download",
                    state = StateRef.of(nextPath)
                )
            )
            .enableBack()
            .update()
    }

    companion object {
        const val UP_DIR = ".."
    }
}

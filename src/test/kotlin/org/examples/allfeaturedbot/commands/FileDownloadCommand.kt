package org.examples.allfeaturedbot.commands

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.Document
import org.github.telegabots.api.annotation.InlineHandler
import java.io.File

class FileDownloadCommand : BaseCommand() {
    @InlineHandler
    fun handle(message: String, messageId: Int, downloadPath: String) {
        log.info("Download file: {}, message: {}", downloadPath, message)

        val file = File(downloadPath)
        context.sendDocument(Document.of(file, caption = "File: $downloadPath"))
    }
}

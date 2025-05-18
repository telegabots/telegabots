package org.examples.testbot.controllers

import org.github.telegabots.MessageFile
import org.github.telegabots.api.BaseController
import org.github.telegabots.api.Document
import org.github.telegabots.api.annotation.InlineHandler
import java.io.File

class FileDownloadController : BaseController() {
    @InlineHandler
    fun handle(message: String, downloadPath: String) {
        log.info("Download file: {}, message: {}", downloadPath, message)

        val file = MessageFile.from(File(downloadPath))
        context.sendDocument(Document.of(file, caption = "File: $downloadPath"))
    }
}

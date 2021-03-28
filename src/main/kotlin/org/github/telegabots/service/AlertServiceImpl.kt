package org.github.telegabots.service

import org.github.telegabots.api.AlertService
import org.github.telegabots.api.ContentType
import org.github.telegabots.api.MessageSender
import org.slf4j.LoggerFactory

class AlertServiceImpl(
    private val messageSender: MessageSender,
    private val alertChatId: Long
) : AlertService {
    private val log = LoggerFactory.getLogger(javaClass)!!

    override fun sendHtmlMessage(message: String, disablePreview: Boolean) {
        if (alertChatId > 0) {
            messageSender.sendMessage(
                alertChatId.toString(), message = message,
                contentType = ContentType.Html, disablePreview = disablePreview
            )
        } else {
            log.info("Alert message: {}", message)
        }
    }

    override fun sendMarkdownMessage(message: String, disablePreview: Boolean) {
        if (alertChatId > 0) {
            messageSender.sendMessage(
                alertChatId.toString(), message = message,
                contentType = ContentType.Markdown, disablePreview = disablePreview
            )
        } else {
            log.info("Alert message: {}", message)
        }
    }
}

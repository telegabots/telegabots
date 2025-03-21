package org.github.telegabots.api

interface AlertService : Service {
    fun sendHtmlMessage(message: String, disablePreview: Boolean = false)

    fun sendMarkdownMessage(message: String, disablePreview: Boolean = false)
}

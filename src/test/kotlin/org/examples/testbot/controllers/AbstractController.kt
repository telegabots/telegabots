package org.examples.testbot.controllers

import org.github.telegabots.api.BaseController
import org.github.telegabots.api.ContentType
import org.github.telegabots.api.MessageType
import org.github.telegabots.api.annotation.ErrorHandler

abstract class AbstractController : BaseController() {
    @ErrorHandler
    open fun handleError(ex: Exception, message: String?) {
        context.page("(Base) Error occurred: *${ex.message}* of type `${ex.javaClass.simpleName}`. With message: `${message ?: "null"}`")
            .contentType(ContentType.Markdown)
            .messageType(MessageType.Inline)
            .enableBack()
            .update()
    }
}
package org.examples.testbot.controllers

import org.github.telegabots.api.ContentType
import org.github.telegabots.api.MessageType
import org.github.telegabots.api.annotation.ErrorHandler
import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler

/**
 * Controller which throws an exception on any request.
 */
class ErrorController : AbstractController() {
    @TextHandler
    fun handle(message: String) {
        throw IllegalStateException("Error in text handler")
    }

    @InlineHandler
    fun handleInline(message: String) {
        throw IllegalStateException("Error in inline handler")
    }

    @ErrorHandler
    override fun handleError(ex: Exception, message: String?) {
        context.page("Common Error occurred: *${ex.message}* of type `${ex.javaClass.simpleName}`. With message: `${message ?: "null"}`")
            .contentType(ContentType.Markdown)
            .messageType(MessageType.Inline)
            .enableBack()
            .update()
    }

    @ErrorHandler
    fun handleRuntimeError(ex: RuntimeException, message: String?) {
        context.page("Error occurred: *${ex.message}* of type `${ex.javaClass.simpleName}`. With message: `${message ?: "null"}`")
            .contentType(ContentType.Markdown)
            .messageType(MessageType.Inline)
            .enableBack()
            .update()
    }
}

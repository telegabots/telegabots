package org.examples.testbot.controllers

import org.github.telegabots.api.BaseController
import org.github.telegabots.api.MessageType
import org.github.telegabots.api.SystemMessages
import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler
import org.github.telegabots.std.cmd.LanguageController

/**
 * Root controller for bot.
 *
 * Handles first message (/start) from user and shows main menu.
 */
class RootController : BaseController() {
    @TextHandler
    fun handle(message: String) {
        if (message == "file") {
            context.create(FileViewController::class.java)
            return
        }

        context.page("RootController: You said: $message")
            .messageType(MessageType.Inline)
            .buttons(FileExplorerController::class.java, FileViewController::class.java, PrintController::class.java, LanguageController::class.java, ErrorController::class.java)
            .create()
    }

    @InlineHandler
    fun handleInline(message: String) {
        if (message == SystemMessages.REFRESH) {
            context.page("RootController: Main menu")
                .messageType(MessageType.Inline)
                .buttons(FileExplorerController::class.java, FileViewController::class.java, PrintController::class.java, LanguageController::class.java, ErrorController::class.java)
                .update()
        } else {
            TODO(message)
        }
    }
}

package org.github.telegabots.handler

import org.github.telegabots.*
import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler
import org.github.telegabots.api.*
import org.github.telegabots.test.scenario
import org.junit.jupiter.api.Test

class MultiSubCommandTests : BaseTests() {
    @Test
    fun testMultipleSubCommands() {
        scenario<CommandRoot> {
            assertThat {
                rootNotCalled()
                userMessageNotSentYet()
                blocksCountEmpty()
            }

            user {
                sendTextMessage("/start")
            }

            assertThat {
                userMessageWasSent()
                rootWasCalled()
                notCalled<SubMenu1Command>()
                blocksCount(1)
                lastBlockPagesCount(1)
            }

            val messageId = lastUserMessageId()

            user {
                sendTextMessage("SUB_MENU1")
            }

            assertThat {
                notCalled<SubMenu1Command>()
                rootWasCalled(2)
                blocksCount(1)
                lastBlockPagesCount(1)
            }

            user {
                sendInlineMessage(messageId = messageId, callbackData = "SUB_MENU1")
            }

            assertThat {
                wasCalled<SubMenu1Command>()
                notCalled<SubMenu2Command>()
                rootWasCalled(2)
                blocksCount(1)
                lastBlockPagesCount(2)
            }

            user {
                sendInlineMessage(messageId = messageId, callbackData = SystemCommands.GO_BACK)
            }

            assertThat {
                wasCalled<SubMenu1Command>(1)
                notCalled<SubMenu2Command>()
                rootWasCalled(3)
                blocksCount(1)
                lastBlockPagesCount(1)
            }

            user {
                sendInlineMessage(messageId = messageId, callbackData = "SUB_MENU2")
            }

            assertThat {
                wasCalled<SubMenu1Command>(1)
                wasCalled<SubMenu2Command>(1)
                rootWasCalled(3)
                blocksCount(1)
                lastBlockPagesCount(2)
            }
        }
    }
}

internal class CommandRoot : BaseCommand() {
    @TextHandler
    fun handle(msg: String) {
        if (msg == "/start") {
            context.createPage(createPage())
        }
    }

    @InlineHandler
    fun handleInline(msg: String) {
        context.updatePage(createPage())
    }

    private fun createPage() = Page(
        message = "Choose menu:",
        contentType = ContentType.Plain,
        messageType = MessageType.Inline,
        subCommands = listOf(listOf(SubCommand.of<SubMenu1Command>(), SubCommand.of<SubMenu2Command>()))
    )
}

internal class SubMenu1Command : BaseCommand() {
    @InlineHandler
    fun handle(message: String) {
        if (message == SystemCommands.REFRESH) {
            context.updatePage(Page("SubMenu1Command", messageType = MessageType.Inline))
        }
    }
}

internal class SubMenu2Command : BaseCommand() {
    @InlineHandler
    fun handle(message: String) {
        if (message == SystemCommands.REFRESH) {
            context.updatePage(Page("SubMenu2Command", messageType = MessageType.Inline))
        }
    }
}

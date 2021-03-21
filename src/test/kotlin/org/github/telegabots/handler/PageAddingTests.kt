package org.github.telegabots.handler

import org.github.telegabots.BaseTests
import org.github.telegabots.CODE_NOT_REACHED
import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.MessageType
import org.github.telegabots.api.Page
import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler
import org.github.telegabots.error.CommandInvokeException
import org.github.telegabots.test.scenario
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class PageAddingTests : BaseTests() {
    @Test
    fun testAddPage_WhenTextMessage() {
        scenario<TextCommandAddingPage> {
            assertThat {
                rootNotCalled()
                blocksCountEmpty()
            }

            user {
                sendTextMessage("/start")
            }

            assertThat {
                rootWasCalled(1)
                blocksCount(1)
                lastBlockPagesCount(1)
            }

            user {
                sendTextMessage("second message")
            }

            assertThat {
                rootWasCalled(2)
                blocksCount(1)
                lastBlockPagesCount(2)
            }
        }
    }

    @Test
    fun testAddPage_WhenInlineMessage() {
        scenario<InlineCommandAddingPage> {
            assertThat {
                rootNotCalled()
                blocksCountEmpty()
            }

            user {
                sendTextMessage("/start")
            }

            val messageId = lastUserMessageId()

            assertThat {
                rootWasCalled(1)
                blocksCount(1)
                lastBlockPagesCount(1)
            }

            user {
                sendInlineMessage(messageId, "someTitleId")
            }

            assertThat {
                rootWasCalled(2)
                blocksCount(1)
                lastBlockPagesCount(2)
            }
        }
    }

    @Test
    fun testFail_AddPage_WhenTextMessageAfterInline() {
        scenario<InvalidCommandTextMessageAfterInline> {
            assertThat {
                rootNotCalled()
                blocksCountEmpty()
            }

            user {
                sendTextMessage("/start")
            }

            val messageId = lastUserMessageId()

            assertThat {
                rootWasCalled(1)
                blocksCount(1)
                lastBlockPagesCount(1)
            }

            user {
                val ex = assertThrows<CommandInvokeException> {
                    sendInlineMessage(messageId, "someTitleId")
                }

                Assertions.assertEquals(
                    "Adding page message type mismatch block's type. Expected: Inline",
                    ex.cause!!.message
                )
                Assertions.assertEquals(IllegalStateException::class.java, ex.cause!!::class.java)
                Assertions.assertEquals(InvalidCommandTextMessageAfterInline::class.java, ex.command)
            }

            assertThat {
                rootWasCalled(1)
                blocksCount(1)
                lastBlockPagesCount(1)
            }
        }
    }

    @Test
    fun testFail_AddPage_WhenInlineMessageAfterText() {
        scenario<InvalidCommandInlineMessageAfterText> {
            assertThat {
                rootNotCalled()
                blocksCountEmpty()
            }

            user {
                sendTextMessage("/start")
            }

            assertThat {
                rootWasCalled(1)
                blocksCount(1)
                lastBlockPagesCount(1)
            }

            user {
                val ex = assertThrows<CommandInvokeException> {
                    sendTextMessage("pupa lupa")
                }

                Assertions.assertEquals(
                    "Adding page message type mismatch block's type. Expected: Text",
                    ex.cause!!.message
                )
                Assertions.assertEquals(IllegalStateException::class.java, ex.cause!!::class.java)
                Assertions.assertEquals(InvalidCommandInlineMessageAfterText::class.java, ex.command)
            }

            assertThat {
                rootWasCalled(1)
                blocksCount(1)
                lastBlockPagesCount(1)
            }
        }
    }
}

internal class TextCommandAddingPage : BaseCommand() {
    @TextHandler
    fun handle(message: String) {
        context.addPage(Page("Hello from command"))
    }
}

internal class InlineCommandAddingPage : BaseCommand() {
    @InlineHandler
    fun handleInline(message: String) {
        context.addPage(Page("new inline content", messageType = MessageType.Inline))
    }

    @TextHandler
    fun handle(message: String) {
        context.addPage(Page("Inline text", messageType = MessageType.Inline))
    }
}

internal class InvalidCommandTextMessageAfterInline : BaseCommand() {
    @TextHandler
    fun handle(message: String) {
        context.addPage(Page("Inline message", messageType = MessageType.Inline))
    }

    @InlineHandler
    fun handleInline(message: String) {
        context.addPage(Page("must be inline but text", messageType = MessageType.Text))
    }
}

internal class InvalidCommandInlineMessageAfterText : BaseCommand() {
    @TextHandler
    fun handle(message: String) {
        if ("/start" == message) {
            context.addPage(Page("Text message", messageType = MessageType.Text))
        } else {
            context.addPage(Page("must be text but inline", messageType = MessageType.Inline))
        }
    }

    @InlineHandler
    fun handleInline(message: String) {
        CODE_NOT_REACHED()
    }
}

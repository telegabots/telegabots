package org.github.telegabots.handler

import org.github.telegabots.BaseTests
import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.MessageType
import org.github.telegabots.api.Page
import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler
import org.github.telegabots.error.CommandInvokeException
import org.github.telegabots.test.scenario
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PageValidationTests : BaseTests() {
    @Test
    fun testFail_WhenTextHandlerNotFound_WhileCreateAddingUpdatingPage() {
        scenario<ValidationTextHandlerRootCommand> {
            assertThat {
                rootNotCalled()
            }

            user {
                sendTextMessage("init block and page")
            }

            assertThat {
                rootWasCalled(1)
            }

            user {
                val ex = assertThrows<CommandInvokeException> { sendTextMessage("create page") }

                assertEquals(
                    "Message handler for type Text in org.github.telegabots.handler.CommandWithOnlyInlineHandler not found. Use annotation @TextHandler",
                    ex.cause!!.message
                )
                assertEquals(IllegalStateException::class.java, ex.cause!!::class.java)
                assertEquals(ValidationTextHandlerRootCommand::class.java, ex.command)
            }

            assertThat {
                notCalled<CommandWithOnlyInlineHandler>()
                rootWasCalled(1)
            }

            user {
                val ex = assertThrows<CommandInvokeException> { sendTextMessage("add page") }

                assertEquals(
                    "Message handler for type Text in org.github.telegabots.handler.CommandWithOnlyInlineHandler not found. Use annotation @TextHandler",
                    ex.cause!!.message
                )
                assertEquals(IllegalStateException::class.java, ex.cause!!::class.java)
                assertEquals(ValidationTextHandlerRootCommand::class.java, ex.command)
            }

            assertThat {
                notCalled<CommandWithOnlyInlineHandler>()
                rootWasCalled(1)
            }

            user {
                val ex = assertThrows<CommandInvokeException> { sendTextMessage("update page") }

                assertEquals(
                    "Message handler for type Text in org.github.telegabots.handler.CommandWithOnlyInlineHandler not found. Use annotation @TextHandler",
                    ex.cause!!.message
                )
                assertEquals(IllegalStateException::class.java, ex.cause!!::class.java)
                assertEquals(ValidationTextHandlerRootCommand::class.java, ex.command)
            }

            assertThat {
                notCalled<CommandWithOnlyInlineHandler>()
                rootWasCalled(1)
            }
        }
    }

    @Test
    fun testFail_WhenInlineHandlerNotFound_WhileCreateAddingUpdatingPage() {
        scenario<ValidationInlineHandlerRootCommand> {
            assertThat {
                rootNotCalled()
            }

            user {
                sendTextMessage("init block and page")
            }

            assertThat {
                rootWasCalled(1)
            }

            user {
                val ex = assertThrows<CommandInvokeException> { sendTextMessage("create page") }

                assertEquals(
                    "Message handler for type Inline in org.github.telegabots.handler.CommandWithOnlyTextHandler not found. Use annotation @InlineHandler",
                    ex.cause!!.message
                )
                assertEquals(IllegalStateException::class.java, ex.cause!!::class.java)
                assertEquals(ValidationInlineHandlerRootCommand::class.java, ex.command)
            }

            assertThat {
                notCalled<CommandWithOnlyInlineHandler>()
                rootWasCalled(1)
            }

            user {
                val ex = assertThrows<CommandInvokeException> { sendTextMessage("add page") }

                assertEquals(
                    "Message handler for type Inline in org.github.telegabots.handler.CommandWithOnlyTextHandler not found. Use annotation @InlineHandler",
                    ex.cause!!.message
                )
                assertEquals(IllegalStateException::class.java, ex.cause!!::class.java)
                assertEquals(ValidationInlineHandlerRootCommand::class.java, ex.command)
            }

            assertThat {
                notCalled<CommandWithOnlyInlineHandler>()
                rootWasCalled(1)
            }

            user {
                val ex = assertThrows<CommandInvokeException> { sendTextMessage("update page") }

                assertEquals(
                    "Message handler for type Inline in org.github.telegabots.handler.CommandWithOnlyTextHandler not found. Use annotation @InlineHandler",
                    ex.cause!!.message
                )
                assertEquals(IllegalStateException::class.java, ex.cause!!::class.java)
                assertEquals(ValidationInlineHandlerRootCommand::class.java, ex.command)
            }

            assertThat {
                notCalled<CommandWithOnlyInlineHandler>()
                rootWasCalled(1)
            }
        }
    }
}

class ValidationTextHandlerRootCommand : BaseCommand() {
    @TextHandler
    fun handle(msg: String) {
        when (msg) {
            "init block and page" -> {
                assertEquals(0, context.blockId()) { "Block id must be 0" }
                assertEquals(0, context.pageId()) { "Block id must be 0" }

                context.updatePage(createInitPage())
            }
            "create page" -> {
                assertTrue(context.blockId() > 0) { "Block id can not be 0" }
                assertTrue(context.pageId() > 0) { "Page id can not be 0" }

                context.createPage(createInvalidPage())
            }
            "add page" -> {
                assertTrue(context.blockId() > 0) { "Block id can not be 0" }
                assertTrue(context.pageId() > 0) { "Page id can not be 0" }

                context.addPage(createInvalidPage())
            }
            "update page" -> {
                assertTrue(context.blockId() > 0) { "Block id can not be 0" }
                assertTrue(context.pageId() > 0) { "Page id can not be 0" }

                context.updatePage(createInvalidPage())
            }
            else -> throw java.lang.IllegalStateException("Unknown test case: $msg")
        }
    }

    private fun createInitPage() = Page("Init text message", messageType = MessageType.Text, handler = this.javaClass)

    private fun createInvalidPage() = Page(
        "Some foo bar", messageType = MessageType.Text, handler = CommandWithOnlyInlineHandler::class.java
    )
}

class CommandWithOnlyInlineHandler : BaseCommand() {
    @InlineHandler
    fun handleInline(msg: String) {
    }
}

class ValidationInlineHandlerRootCommand : BaseCommand() {
    @TextHandler
    fun handle(msg: String) {
        when (msg) {
            "init block and page" -> {
                assertEquals(0, context.blockId()) { "Block id must be 0" }
                assertEquals(0, context.pageId()) { "Block id must be 0" }

                context.updatePage(createInitPage())
            }
            "create page" -> {
                assertTrue(context.blockId() > 0) { "Block id can not be 0" }
                assertTrue(context.pageId() > 0) { "Page id can not be 0" }

                context.createPage(createInvalidPage())
            }
            "add page" -> {
                assertTrue(context.blockId() > 0) { "Block id can not be 0" }
                assertTrue(context.pageId() > 0) { "Page id can not be 0" }

                context.addPage(createInvalidPage())
            }
            "update page" -> {
                assertTrue(context.blockId() > 0) { "Block id can not be 0" }
                assertTrue(context.pageId() > 0) { "Page id can not be 0" }

                context.updatePage(createInvalidPage())
            }
            else -> throw java.lang.IllegalStateException("Unknown test case: $msg")
        }
    }

    private fun createInitPage() = Page("Init text message", messageType = MessageType.Text, handler = this.javaClass)

    private fun createInvalidPage() = Page(
        "Some foo bar", messageType = MessageType.Inline, handler = CommandWithOnlyTextHandler::class.java
    )
}

class CommandWithOnlyTextHandler : BaseCommand() {
    @TextHandler
    fun handleText(msg: String) {
    }
}

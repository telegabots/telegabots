package org.github.telegabots.handler

import org.github.telegabots.BaseTests
import org.github.telegabots.api.*
import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler
import org.github.telegabots.test.scenario
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StateParamTests : BaseTests() {
    @Test
    fun testCommand_Success_WhenHandlerWithStateParam() {
        scenario<CommandWithStateParam> {
            assertFalse(CommandWithStateParam.handlerCalled.get())

            user {
                sendTextMessage("Hello from client")
            }

            assertThat {
                commandReturnTrue()
                assertTrue(CommandWithStateParam.handlerCalled.get())
            }
        }
    }

    @Test
    fun testCommand_Success_WhenInlineHandlerWithStateParam() {
        scenario<CommandWithStateParam> {
            assertFalse(CommandWithStateParam.inlineHandlerCalled.get())

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
                sendInlineMessage(messageId = messageId, callbackData = "Hello from client callback")
            }

            assertThat {
                commandReturnTrue()
                assertTrue(CommandWithStateParam.inlineHandlerCalled.get())
            }
        }
    }

    @Test
    fun testCommand_Success_WhenHandlerWithReadonlyLocalStateParam() {
        scenario<CommandWithReadonlyLocalState> {
            assertFalse(CommandWithReadonlyLocalState.handlerCalled.get())

            user {
                sendTextMessage("Hello!")
            }

            assertThat {
                commandReturnTrue()
                assertTrue(CommandWithReadonlyLocalState.handlerCalled.get())
            }
        }
    }

    @Test
    fun testCommand_Success_WhenInlineHandlerShouldNotCalledWhenBlockByMessageIdNotFound() {
        scenario<CommandWithReadonlyLocalState> {
            assertFalse(CommandWithReadonlyLocalState.inlineHandlerCalled.get())

            user {
                sendInlineMessage(messageId = 123987, callbackData = "Data2")
            }

            assertThat {
                commandReturnTrue()
                assertFalse(CommandWithReadonlyLocalState.inlineHandlerCalled.get(), "Inline handler called but not expected")
            }
        }
    }

    @Test
    fun testCommand_Success_WhenInlineHandlerCalledWhenBlockByMessageIdFound() {
        scenario<CommandWithReadonlyLocalState> {
            assertFalse(CommandWithReadonlyLocalState.inlineHandlerCalled.get())

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
                // first send inline message with unknown messageId
                sendInlineMessage(messageId = 123987, callbackData = "Data2")
            }

            assertThat {
                commandReturnTrue()
                assertFalse(CommandWithReadonlyLocalState.inlineHandlerCalled.get(), "Inline handler called but not expected")
            }

            user {
                // send inline message with real messageId
                sendInlineMessage(messageId = messageId, callbackData = "Data2")
            }

            assertThat {
                commandReturnTrue()
                assertTrue(CommandWithReadonlyLocalState.inlineHandlerCalled.get())
            }
        }
    }

    @Test
    fun testCommand_DontReuseCachedLocalState() {
        scenario<CommandReuseLocalState> {
            user {
                sendTextMessage("HELLO!")
            }

            assertThat {
                rootWasCalled(1)
                userMessageWasSent()
            }

            val messageId = lastUserMessageId()

            user {
                sendTextMessage("BYE!")
            }

            assertThat {
                rootWasCalled(2)
                userMessageWasSent()
            }

            user {
                sendInlineMessage(messageId = messageId, "CMD1")

            }

            assertThat {
                rootWasCalled(3)
            }
        }
    }
}

internal class CommandWithStateParam : BaseCommand() {
    @TextHandler
    fun handleText(msg: String, intState: State<Int>) {
        if (msg == MESSAGE_START) {
            context.addPage(Page("Inline text", messageType = MessageType.Inline))
            return
        }

        assertEquals("Hello from client", msg)
        assertNull(intState.get())
        assertFalse(intState.isPresent())

        intState.set(42777)

        assertEquals(42777, intState.get())
        assertTrue(intState.isPresent())

        handlerCalled.set(true)
    }

    @InlineHandler
    fun handleInline(msg: String, doubleState: State<Double>) {
        assertEquals("Hello from client callback", msg)
        assertEquals(100001, context.messageId())
        assertFalse(doubleState.isPresent())
        assertNull(doubleState.get())

        doubleState.set(12.5)

        assertEquals(12.5, doubleState.get())
        assertTrue(doubleState.isPresent())

        inlineHandlerCalled.set(true)
    }

    companion object {
        val handlerCalled = AtomicBoolean()
        val inlineHandlerCalled = AtomicBoolean()
    }
}

internal class CommandWithReadonlyLocalState : BaseCommand() {
    @TextHandler
    fun handle(message: String, readOnlyState: String?) {
        assertNull(readOnlyState)

        if (message == MESSAGE_START) {
            context.addPage(Page("Inline text", messageType = MessageType.Inline))
        } else {
            assertEquals("Hello!", message)
        }

        handlerCalled.set(true)
    }

    @InlineHandler
    fun handleInline(message: String, readOnlyState: String?) {
        assertEquals("Data2", message)
        assertEquals(100001, context.messageId())
        assertNull(readOnlyState)

        inlineHandlerCalled.set(true)
    }

    companion object {
        val handlerCalled = AtomicBoolean()
        val inlineHandlerCalled = AtomicBoolean()
    }
}

internal class CommandReuseLocalState : BaseCommand() {
    @TextHandler
    fun handle(message: String) {
        context.createPage(
            Page(
                "Some message", ContentType.Plain, MessageType.Inline,
                subCommands = listOf(listOf(SubCommand.of("CMD1", state = StateRef.of("FOO"))))
            )
        )
    }

    @InlineHandler
    fun handleInline(commandId: String, state: State<String>) {
        assertEquals("FOO", state.get())
    }
}

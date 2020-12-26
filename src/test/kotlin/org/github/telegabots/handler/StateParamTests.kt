package org.github.telegabots.handler

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.BaseTests
import org.github.telegabots.api.State
import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StateParamTests : BaseTests() {
    @Test
    fun testCommand_Success_WhenHandlerWithStateParam() {
        val executor = createExecutor(CommandWithStateParam::class.java)
        val update = createAnyTextMessage(messageText = "Hello from client")

        assertFalse(CommandWithStateParam.handlerCalled.get())

        val success = executor.handle(update)

        assertTrue(success)
        assertTrue(CommandWithStateParam.handlerCalled.get())
    }

    @Test
    fun testCommand_Success_WhenInlineHandlerWithStateParam() {
        val executor = createExecutor(CommandWithStateParam::class.java)
        val update = createAnyInlineMessage(messageId = 445577, callbackData = "Hello from client callback")

        assertFalse(CommandWithStateParam.inlineHandlerCalled.get())

        val success = executor.handle(update)

        assertTrue(success)
        assertTrue(CommandWithStateParam.inlineHandlerCalled.get())
    }

    @Test
    fun testCommand_Success_WhenHandlerWithReadonlyLocalStateParam() {
        val executor = createExecutor(CommandWithReadonlyLocalState::class.java)
        val update = createAnyTextMessage(messageText = "Hello!")

        assertFalse(CommandWithReadonlyLocalState.handlerCalled.get())

        val success = executor.handle(update)

        assertTrue(success)
        assertTrue(CommandWithReadonlyLocalState.handlerCalled.get())
    }

    @Test
    fun testCommand_Success_WhenHandlerWithInlineReadonlyLocalStateParam() {
        val executor = createExecutor(CommandWithReadonlyLocalState::class.java)
        val update = createAnyInlineMessage(messageId = 123987, callbackData = "Data2")

        assertFalse(CommandWithReadonlyLocalState.inlineHandlerCalled.get())

        val success = executor.handle(update)

        assertTrue(success)
        assertTrue(CommandWithReadonlyLocalState.inlineHandlerCalled.get())
    }
}

internal class CommandWithStateParam : BaseCommand() {
    @TextHandler
    fun handleText(msg: String, intState: State<Int>) {
        assertEquals("Hello from client", msg)
        assertNull(intState.get())
        assertFalse(intState.isPresent())

        intState.set(42777)

        assertEquals(42777, intState.get())
        assertTrue(intState.isPresent())

        handlerCalled.set(true)
    }

    @InlineHandler
    fun handleInline(msg: String, messageId: Int, doubleState: State<Double>) {
        assertEquals("Hello from client callback", msg)
        assertEquals(445577, messageId)
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
        assertEquals("Hello!", message)
        assertNull(readOnlyState)

        handlerCalled.set(true)
    }

    @InlineHandler
    fun handleInline(messageId: Int, message: String, readOnlyState: String?) {
        assertEquals("Data2", message)
        assertEquals(123987, messageId)
        assertNull(readOnlyState)

        inlineHandlerCalled.set(true)
    }

    companion object {
        val handlerCalled = AtomicBoolean()
        val inlineHandlerCalled = AtomicBoolean()
    }
}

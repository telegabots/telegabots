package org.github.telegabots.handler

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.BaseTests
import org.github.telegabots.api.State
import org.github.telegabots.api.annotation.CallbackHandler
import org.github.telegabots.api.annotation.CommandHandler
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
        val update = createAnyMessage(messageText = "Hello from client")

        assertFalse(CommandWithStateParam.handlerCalled.get())

        val success = executor.handle(update)

        assertTrue(success)
        assertTrue(CommandWithStateParam.handlerCalled.get())
    }

    @Test
    fun testCommand_Success_WhenCallbackHandlerWithStateParam() {
        val executor = createExecutor(CommandWithStateParam::class.java)
        val update = createAnyCallbackMessage(messageId = 445577, callbackData = "Hello from client callback")

        assertFalse(CommandWithStateParam.callbackHandlerCalled.get())

        val success = executor.handle(update)

        assertTrue(success)
        assertTrue(CommandWithStateParam.callbackHandlerCalled.get())
    }

    @Test
    fun testCommand_Success_WhenHandlerWithReadonlyLocalStateParam() {
        val executor = createExecutor(CommandWithReadonlyLocalState::class.java)
        val update = createAnyMessage(messageText = "Hello!")

        assertFalse(CommandWithReadonlyLocalState.handlerCalled.get())

        val success = executor.handle(update)

        assertTrue(success)
        assertTrue(CommandWithReadonlyLocalState.handlerCalled.get())
    }

    @Test
    fun testCommand_Success_WhenHandlerWithCallbackReadonlyLocalStateParam() {
        val executor = createExecutor(CommandWithReadonlyLocalState::class.java)
        val update = createAnyCallbackMessage(messageId = 123987, callbackData = "Data2")

        assertFalse(CommandWithReadonlyLocalState.callbackHandlerCalled.get())

        val success = executor.handle(update)

        assertTrue(success)
        assertTrue(CommandWithReadonlyLocalState.callbackHandlerCalled.get())
    }
}

internal class CommandWithStateParam : BaseCommand() {
    @CommandHandler
    fun handle(msg: String, intState: State<Int>) {
        assertEquals("Hello from client", msg)
        assertNull(intState.get())
        assertFalse(intState.isPresent())

        intState.set(42777)

        assertEquals(42777, intState.get())
        assertTrue(intState.isPresent())

        handlerCalled.set(true)
    }

    @CallbackHandler
    fun handleCallback(msg: String, messageId: Int, doubleState: State<Double>) {
        assertEquals("Hello from client callback", msg)
        assertEquals(445577, messageId)
        assertFalse(doubleState.isPresent())
        assertNull(doubleState.get())

        doubleState.set(12.5)

        assertEquals(12.5, doubleState.get())
        assertTrue(doubleState.isPresent())

        callbackHandlerCalled.set(true)
    }

    companion object {
        val handlerCalled = AtomicBoolean()
        val callbackHandlerCalled = AtomicBoolean()
    }
}

internal class CommandWithReadonlyLocalState : BaseCommand() {
    @CommandHandler
    fun handle(message: String, readOnlyState: String?) {
        assertEquals("Hello!", message)
        assertNull(readOnlyState)

        handlerCalled.set(true)
    }

    @CallbackHandler
    fun handleCallback(messageId: Int, message: String, readOnlyState: String?) {
        assertEquals("Data2", message)
        assertEquals(123987, messageId)
        assertNull(readOnlyState)

        callbackHandlerCalled.set(true)
    }

    companion object {
        val handlerCalled = AtomicBoolean()
        val callbackHandlerCalled = AtomicBoolean()
    }
}

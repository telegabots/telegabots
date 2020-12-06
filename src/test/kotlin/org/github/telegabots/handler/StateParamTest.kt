package org.github.telegabots.handler

import org.github.telegabots.BaseCommand
import org.github.telegabots.BaseTests
import org.github.telegabots.State
import org.github.telegabots.annotation.CallbackHandler
import org.github.telegabots.annotation.CommandHandler
import org.github.telegabots.exectutor.BotCommandExecutor
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StateParamTest : BaseTests() {
    @Test
    fun testCommand_Success_WhenHandlerWithStateParam() {
        val executor = BotCommandExecutor(rootCommand = CommandWithStateParam::class.java)
        val update = createAnyMessage(messageText = "Hello from client")

        assertEquals(0, CommandWithStateParam.handleCounter.get())

        val success = executor.handle(update)

        assertTrue { success }
        assertEquals(1, CommandWithStateParam.handleCounter.get())
    }

    @Test
    fun testCommand_Success_WhenCallbackHandlerWithStateParam() {
        val executor = BotCommandExecutor(rootCommand = CommandWithStateParam::class.java)
        val update = createAnyCallbackMessage(messageId = 445577, callbackData = "Hello from client callback")

        assertEquals(0, CommandWithStateParam.handleCallbackCounter.get())

        val success = executor.handle(update)

        assertTrue { success }
        assertEquals(1, CommandWithStateParam.handleCallbackCounter.get())
    }
}

internal class CommandWithStateParam : BaseCommand() {
    @CommandHandler
    fun handle(msg: String, intState: State<Int>) {
        assertEquals("Hello from client", msg)
        assertNull(intState.get())
        intState.set(42777)
        assertEquals(42777, intState.get())
        handleCounter.incrementAndGet()
    }

    @CallbackHandler
    fun handleCallback(msg: String, messageId: Int, doubleState: State<Double>) {
        assertEquals("Hello from client callback", msg)
        assertEquals(445577, messageId)
        assertNull(doubleState.get())
        doubleState.set(12.5)
        assertEquals(12.5, doubleState.get())
        handleCallbackCounter.incrementAndGet()
    }

    companion object {
        val handleCounter = AtomicInteger()
        val handleCallbackCounter = AtomicInteger()
    }
}

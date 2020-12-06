package org.github.telegabots.simple

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.github.telegabots.BaseCommand
import org.github.telegabots.CODE_NOT_REACHED
import org.github.telegabots.annotation.CallbackHandler
import org.github.telegabots.exectutor.BotCommandExecutor
import kotlin.test.assertEquals

class CallbackHandlerTests : BaseTests() {
    @Test
    fun testCommand_Success_WhenHandlerNotReturnsBool() {
        val executor = BotCommandExecutor(rootCommand = InvalidCallbackCommandWithoutAnyParam::class.java)
        val update = createAnyMessage()
        val ex = assertThrows<IllegalStateException> { executor.handle(update) }

        assertEquals(
            "Handler must contains at least two parameters: public final void org.github.telegabots.simple.InvalidCallbackCommandWithoutAnyParam.handle()",
            ex.message
        )
    }
}

internal class InvalidCallbackCommandWithoutAnyParam : BaseCommand() {
    @CallbackHandler
    fun handle() {
        CODE_NOT_REACHED()
    }
}

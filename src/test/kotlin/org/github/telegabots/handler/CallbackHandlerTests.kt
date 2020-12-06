package org.github.telegabots.handler

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.github.telegabots.BaseCommand
import org.github.telegabots.BaseTests
import org.github.telegabots.CODE_NOT_REACHED
import org.github.telegabots.annotation.CallbackHandler
import org.github.telegabots.exectutor.BotCommandExecutor
import kotlin.test.assertEquals

class CallbackHandlerTests : BaseTests() {
    @Test
    fun testCommand_Fail_WhenHandlerWithoutParams() {
        val executor = BotCommandExecutor(rootCommand = InvalidCallbackCommandWithoutAnyParam::class.java)
        val update = createAnyMessage()
        val ex = assertThrows<IllegalStateException> { executor.handle(update) }

        assertEquals(
            "Handler must contains at least two parameters: public final void org.github.telegabots.handler.InvalidCallbackCommandWithoutAnyParam.handle()",
            ex.message
        )
    }

    @Test
    fun testCommand_Fail_WhenHandlerWithOnlyIntParam() {
        val executor = BotCommandExecutor(rootCommand = InvalidCallbackCommandWithOnlyIntParam::class.java)
        val update = createAnyMessage()
        val ex = assertThrows<IllegalStateException> { executor.handle(update) }

        assertEquals(
            "Handler must contains at least two parameters: public final void org.github.telegabots.handler.InvalidCallbackCommandWithOnlyIntParam.handle(int)",
            ex.message
        )
    }

    @Test
    fun testCommand_Fail_WhenHandlerWithOnlyTwoIntParams() {
        val executor = BotCommandExecutor(rootCommand = InvalidCallbackCommandWithTwoIntParams::class.java)
        val update = createAnyMessage()
        val ex = assertThrows<IllegalStateException> { executor.handle(update) }

        assertEquals(
            "First two parameters must be Integer and String (or vice versa) in handler public final void org.github.telegabots.handler.InvalidCallbackCommandWithTwoIntParams.handle(int,int)",
            ex.message
        )
    }

    @Test
    fun testCommand_Fail_WhenHandlerWithOnlyTwoStringParams() {
        val executor = BotCommandExecutor(rootCommand = InvalidCallbackCommandWithTwoStringParams::class.java)
        val update = createAnyMessage()
        val ex = assertThrows<IllegalStateException> { executor.handle(update) }

        assertEquals(
            "First two parameters must be Integer and String (or vice versa) in handler public final void org.github.telegabots.handler.InvalidCallbackCommandWithTwoStringParams.handle(java.lang.String,java.lang.String)",
            ex.message
        )
    }

    @Test
    fun testCommand_Success_WhenHandlerWithIntStringParams() {
        val executor = BotCommandExecutor(rootCommand = ValidCallbackCommandIntString::class.java)
        val update = createAnyCallbackMessage(messageId = 4273, callbackData = "IntString")
        executor.handle(update)
    }

    @Test
    fun testCommand_Success_WhenHandlerWithStringIntParams() {
        val executor = BotCommandExecutor(rootCommand = ValidCallbackCommandStringInt::class.java)
        val update = createAnyCallbackMessage(messageId = 55557, callbackData = "StringInt")
        executor.handle(update)
    }
}

internal class InvalidCallbackCommandWithoutAnyParam : BaseCommand() {
    @CallbackHandler
    fun handle() {
        CODE_NOT_REACHED()
    }
}

internal class InvalidCallbackCommandWithOnlyIntParam() : BaseCommand() {
    @CallbackHandler
    fun handle(messageId: Int) {
        CODE_NOT_REACHED()
    }
}

internal class InvalidCallbackCommandWithTwoIntParams() : BaseCommand() {
    @CallbackHandler
    fun handle(first: Int, second: Int) {
        CODE_NOT_REACHED()
    }
}

internal class InvalidCallbackCommandWithTwoStringParams() : BaseCommand() {
    @CallbackHandler
    fun handle(first: String, second: String) {
        CODE_NOT_REACHED()
    }
}

internal class ValidCallbackCommandIntString() : BaseCommand() {
    @CallbackHandler
    fun handle(message: String, messageId: Int) {
        assertEquals(4273, messageId)
        assertEquals("IntString", message)
    }
}

internal class ValidCallbackCommandStringInt() : BaseCommand() {
    @CallbackHandler
    fun handle(message: String, messageId: Int) {
        assertEquals(55557, messageId)
        assertEquals("StringInt", message)
    }
}

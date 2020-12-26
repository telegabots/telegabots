package org.github.telegabots.handler

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.BaseTests
import org.github.telegabots.CODE_NOT_REACHED
import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.test.CommandAssert.assertNotCalled
import org.github.telegabots.test.CommandAssert.assertWasCalled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class InlineHandlerTests : BaseTests() {
    @Test
    fun testCommand_Fail_WhenHandlerWithoutParams() {
        val executor = createExecutor(InvalidInlineCommandWithoutAnyParam::class.java)
        val update = createAnyTextMessage()
        val ex = assertThrows<IllegalStateException> { executor.handle(update) }

        assertEquals(
            "Handler must contains at least two parameters: public final void org.github.telegabots.handler.InvalidInlineCommandWithoutAnyParam.handle()",
            ex.message
        )
    }

    @Test
    fun testCommand_Fail_WhenHandlerWithOnlyIntParam() {
        val executor = createExecutor(InvalidInlineCommandWithOnlyIntParam::class.java)
        val update = createAnyTextMessage()
        val ex = assertThrows<IllegalStateException> { executor.handle(update) }

        assertEquals(
            "Handler must contains at least two parameters: public final void org.github.telegabots.handler.InvalidInlineCommandWithOnlyIntParam.handle(int)",
            ex.message
        )
    }

    @Test
    fun testCommand_Fail_WhenHandlerWithOnlyTwoIntParams() {
        val executor = createExecutor(InvalidInlineCommandWithTwoIntParams::class.java)
        val update = createAnyTextMessage()
        val ex = assertThrows<IllegalStateException> { executor.handle(update) }

        assertEquals(
            "First two parameters must be Integer and String (or vice versa) in handler public final void org.github.telegabots.handler.InvalidInlineCommandWithTwoIntParams.handle(int,int)",
            ex.message
        )
    }

    @Test
    fun testCommand_Fail_WhenHandlerWithOnlyTwoStringParams() {
        val executor = createExecutor(InvalidInlineCommandWithTwoStringParams::class.java)
        val update = createAnyTextMessage()
        val ex = assertThrows<IllegalStateException> { executor.handle(update) }

        assertEquals(
            "First two parameters must be Integer and String (or vice versa) in handler public final void org.github.telegabots.handler.InvalidInlineCommandWithTwoStringParams.handle(java.lang.String,java.lang.String)",
            ex.message
        )
    }

    @Test
    fun testCommand_Success_WhenHandlerWithIntStringParams() {
        val executor = createExecutor(ValidInlineCommandIntString::class.java)
        val update = createAnyInlineMessage(messageId = 4273, callbackData = "IntString")

        assertNotCalled<ValidInlineCommandIntString>()

        executor.handle(update)

        assertWasCalled<ValidInlineCommandIntString>()
    }

    @Test
    fun testCommand_Success_WhenHandlerWithStringIntParams() {
        val executor = createExecutor(ValidInlineCommandStringInt::class.java)
        val update = createAnyInlineMessage(messageId = 55557, callbackData = "StringInt")

        assertNotCalled<ValidInlineCommandStringInt>()

        executor.handle(update)

        assertWasCalled<ValidInlineCommandStringInt>()
    }
}

internal class InvalidInlineCommandWithoutAnyParam : BaseCommand() {
    @InlineHandler
    fun handle() {
        CODE_NOT_REACHED()
    }
}

internal class InvalidInlineCommandWithOnlyIntParam() : BaseCommand() {
    @InlineHandler
    fun handle(messageId: Int) {
        CODE_NOT_REACHED()
    }
}

internal class InvalidInlineCommandWithTwoIntParams() : BaseCommand() {
    @InlineHandler
    fun handle(first: Int, second: Int) {
        CODE_NOT_REACHED()
    }
}

internal class InvalidInlineCommandWithTwoStringParams() : BaseCommand() {
    @InlineHandler
    fun handle(first: String, second: String) {
        CODE_NOT_REACHED()
    }
}

internal class ValidInlineCommandIntString() : BaseCommand() {
    @InlineHandler
    fun handle(message: String, messageId: Int) {
        assertEquals(4273, messageId)
        assertEquals("IntString", message)
    }
}

internal class ValidInlineCommandStringInt() : BaseCommand() {
    @InlineHandler
    fun handle(message: String, messageId: Int) {
        assertEquals(55557, messageId)
        assertEquals("StringInt", message)
    }
}

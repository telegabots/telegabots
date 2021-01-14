package org.github.telegabots.handler

import org.github.telegabots.BaseTests
import org.github.telegabots.CODE_NOT_REACHED
import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler
import org.github.telegabots.test.scenario
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InlineHandlerTests : BaseTests() {
    @Test
    fun testCommand_Fail_WhenHandlerWithoutParams() {
        val ex = assertThrows<IllegalStateException> {
            scenario<InvalidInlineCommandWithoutAnyParam> { }
        }

        assertEquals(
            "Handler must contains at least one parameter: public final void org.github.telegabots.handler.InvalidInlineCommandWithoutAnyParam.handle()",
            ex.message
        )
    }

    @Test
    fun testCommand_Fail_WhenHandlerWithOnlyIntParam() {
        val ex = assertThrows<IllegalStateException> {
            scenario<InvalidInlineCommandWithOnlyIntParam> { }
        }

        assertEquals(
            "First parameter must be String but found int in handler public final void org.github.telegabots.handler.InvalidInlineCommandWithOnlyIntParam.handle(int)",
            ex.message
        )
    }

    @Test
    fun testCommand_Fail_WhenHandlerWithOnlyTwoIntParams() {
        val ex = assertThrows<IllegalStateException> {
            scenario<InvalidInlineCommandWithTwoIntParams> { }
        }

        assertEquals(
            "First parameter must be String but found int in handler public final void org.github.telegabots.handler.InvalidInlineCommandWithTwoIntParams.handle(int,int)",
            ex.message
        )
    }

    @Test
    fun testCommand_Fail_WhenHandlerWithOnlyTwoStringParams() {
        scenario<ValidInlineCommandWithTwoStringParams> {
            assertThat {
                notCalled<ValidInlineCommandWithTwoStringParams>()
            }

            user { sendInlineMessage(12357, "XXX") }

            assertThat {
                wasCalled<ValidInlineCommandWithTwoStringParams>(1)
            }
        }
    }

    @Test
    fun testCommand_Success_WhenHandlerWithStringIntParams() {
        scenario<ValidInlineCommandStringInt> {
            assertThat {
                notCalled<ValidInlineCommandStringInt>()
            }

            user {
                sendInlineMessage(messageId = 55557, callbackData = "StringInt")
            }

            assertThat {
                wasCalled<ValidInlineCommandStringInt>(1)
            }
        }
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

internal class ValidInlineCommandWithTwoStringParams() : BaseCommand() {
    @InlineHandler
    fun handle(first: String, second: String?) {
        assertEquals(12357, context.inlineMessageId())
        assertEquals("XXX", first)
        assertNull(second)
    }

    @TextHandler
    fun handle(message: String) {
        CODE_NOT_REACHED()
    }
}

internal class ValidInlineCommandStringInt() : BaseCommand() {
    @InlineHandler
    fun handleInline(message: String, someInt: Int?) {
        assertEquals(55557, context.inlineMessageId())
        assertEquals("StringInt", message)
        assertNull(someInt)
    }

    @TextHandler
    fun handle(message: String) {
    }
}

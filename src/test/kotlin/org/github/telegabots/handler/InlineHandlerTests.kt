package org.github.telegabots.handler

import org.github.telegabots.BaseTests
import org.github.telegabots.CODE_NOT_REACHED
import org.github.telegabots.api.BaseController
import org.github.telegabots.api.MessageType
import org.github.telegabots.api.Page
import org.github.telegabots.api.Button
import org.github.telegabots.api.SystemMessages
import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler
import org.github.telegabots.test.scenario
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InlineHandlerTests : BaseTests() {
    @Test
    fun testController_Fail_WhenHandlerWithoutParams() {
        val ex = assertThrows<IllegalStateException> {
            scenario<InvalidInlineControllerWithoutAnyParam> { }
        }

        assertEquals(
            "Handler must contains at least one parameter: public final void org.github.telegabots.handler.InvalidInlineControllerWithoutAnyParam.handle()",
            ex.message
        )
    }

    @Test
    fun testController_Fail_WhenHandlerWithOnlyIntParam() {
        val ex = assertThrows<IllegalStateException> {
            scenario<InvalidInlineControllerWithOnlyIntParam> { }
        }

        assertEquals(
            "First parameter must be String but found int in handler public final void org.github.telegabots.handler.InvalidInlineControllerWithOnlyIntParam.handle(int)",
            ex.message
        )
    }

    @Test
    fun testController_Fail_WhenHandlerWithOnlyTwoIntParams() {
        val ex = assertThrows<IllegalStateException> {
            scenario<InvalidInlineControllerWithTwoIntParams> { }
        }

        assertEquals(
            "First parameter must be String but found int in handler public final void org.github.telegabots.handler.InvalidInlineControllerWithTwoIntParams.handle(int,int)",
            ex.message
        )
    }

    @Test
    fun testController_Fail_WhenHandlerWithOnlyTwoStringParams() {
        scenario<ValidInlineControllerWithTwoStringParams> {
            assertThat {
                notCalled<ValidInlineControllerWithTwoStringParams>()
            }

            user {
                sendTextMessage("/start")
            }

            val messageId = lastUserMessageId()

            assertThat {
                rootWasCalled(1)
                blocksCount(1)
                lastBlockPagesCount(1)
                wasCalled<ValidInlineControllerWithTwoStringParams>(1)
            }

            user { sendInlineMessage(messageId, "XXX") }

            assertThat {
                wasCalled<ValidInlineControllerWithTwoStringParams>(2)
            }
        }
    }

    @Test
    fun testController_Success_WhenHandlerWithStringIntParams() {
        scenario<ValidInlineControllerStringInt> {
            assertThat {
                notCalled<ValidInlineControllerStringInt>()
            }

            user {
                sendTextMessage("/start")
            }

            val messageId = lastUserMessageId()

            assertThat {
                rootWasCalled(1)
                blocksCount(1)
                lastBlockPagesCount(1)
                wasCalled<ValidInlineControllerStringInt>(1)
            }

            user {
                sendInlineMessage(messageId = messageId, callbackData = "StringInt")
            }

            assertThat {
                wasCalled<ValidInlineControllerStringInt>(2)
            }
        }
    }

    @Test
    fun testController_Success_RedirectToRootController_WhenTextHandlerNotExists() {
        scenario<ValidTextHandlerController> {
            assertThat {
                notCalled<ValidTextHandlerController>()
            }

            user {
                sendTextMessage("start")
            }

            assertThat {
                wasCalled<ValidTextHandlerController>(1)
                notCalled<ValidInlineHandlerController>()
            }

            user {
                sendInlineMessage(messageId = lastUserMessageId(), callbackData = "VALID_INLINE_HANDLER")
            }

            assertThat {
                wasCalled<ValidTextHandlerController>(1)
                wasCalled<ValidInlineHandlerController>(1)
            }

            user {
                sendTextMessage("this is text command")
            }

            assertThat {
                wasCalled<ValidTextHandlerController>(2)
                wasCalled<ValidInlineHandlerController>(1)
            }
        }
    }
}

internal class InvalidInlineControllerWithoutAnyParam : BaseController() {
    @InlineHandler
    fun handle() {
        CODE_NOT_REACHED()
    }
}

internal class InvalidInlineControllerWithOnlyIntParam() : BaseController() {
    @InlineHandler
    fun handle(messageId: Int) {
        CODE_NOT_REACHED()
    }
}

internal class InvalidInlineControllerWithTwoIntParams() : BaseController() {
    @InlineHandler
    fun handle(first: Int, second: Int) {
        CODE_NOT_REACHED()
    }
}

internal class ValidInlineControllerWithTwoStringParams() : BaseController() {
    @InlineHandler
    fun handle(first: String, second: String?) {
        assertEquals(100001, context.messageId())
        assertEquals("XXX", first)
        assertNull(second)
    }

    @TextHandler
    fun handle(message: String) {
        if (message == MESSAGE_START) {
            context.addPage(Page("Inline text", messageType = MessageType.Inline))
        } else {
            CODE_NOT_REACHED()
        }
    }
}

internal class ValidInlineControllerStringInt() : BaseController() {
    @InlineHandler
    fun handleInline(message: String, someInt: Int?) {
        assertEquals(100001, context.messageId())
        assertEquals("StringInt", message)
        assertNull(someInt)
    }

    @TextHandler
    fun handle(message: String) {
        if (message == MESSAGE_START) {
            context.addPage(Page("Inline text", messageType = MessageType.Inline))
        }
    }
}


internal class ValidTextHandlerController() : BaseController() {
    @TextHandler
    fun handle(message: String) {
        if ("start" == message) {
            context.createPage(
                Page(
                    "INLINE ME", messageType = MessageType.Inline,
                    buttons = listOf(
                        listOf(
                            Button.of(ValidInlineHandlerController::class.java),
                            Button.REFRESH
                        )
                    )
                )
            )
        } else {
            assertEquals("this is text command", message)
        }
    }
}

internal class ValidInlineHandlerController : BaseController() {
    @InlineHandler
    fun handleInline(message: String, someInt: Int?) {
        assertEquals(SystemMessages.REFRESH, message)
    }
}

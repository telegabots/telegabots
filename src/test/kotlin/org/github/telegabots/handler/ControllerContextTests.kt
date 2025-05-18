package org.github.telegabots.handler

import org.github.telegabots.BaseTests
import org.github.telegabots.CODE_NOT_REACHED
import org.github.telegabots.api.*
import org.github.telegabots.api.annotation.TextHandler
import org.github.telegabots.test.ControllerAssert.assertNotCalled
import org.github.telegabots.test.ControllerAssert.resetCalled
import org.github.telegabots.test.scenario
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ControllerContextTests : BaseTests() {
    @Test
    fun testWhenInnerControllerInvoked() {
        scenario<ControllerUsesControllerContext> {
            user {
                resetCalled<ControllerUsesControllerContext>()
                addLocalization("commandId1" to "Command Id Title")
                sendTextMessage("foo bar 2")
            }

            assertThat {
                wasCalled<ControllerUsesControllerContext>(1)
                notCalled<AnotherController>()
            }

            user {
                sendTextMessage("Command Id Title")
            }

            assertThat {
                wasCalled<ControllerUsesControllerContext>(1)
                wasCalled<AnotherController>(1)
            }
        }
    }

    @Test
    fun testWhenInnerControllerNotInvoked() {
        scenario<ControllerUsesControllerContext> {
            user {
                resetCalled<ControllerUsesControllerContext>()
                addLocalization("commandId1" to "Command Id Title")
                sendTextMessage("foo bar 2")
            }

            assertThat {
                wasCalled<ControllerUsesControllerContext>(1)
                notCalled<AnotherController>()
            }

            user {
                sendTextMessage("Command Id Title 2")
            }

            assertThat {
                wasCalled<ControllerUsesControllerContext>(2)
                notCalled<AnotherController>()
            }
        }
    }

    @Test
    fun testController_Fail_WhenHandlerUseControllerContextAsParam() {
        val ex = assertThrows<IllegalStateException> { scenario<ControllerWithControllerContextParam> {}}

        assertEquals(
            "ControllerContext can not be used as handler parameter. Use \"context\" field instead. Handler: public final void org.github.telegabots.handler.ControllerWithControllerContextParam.handle(java.lang.String,org.github.telegabots.api.ControllerContext)",
            ex.message
        )
        assertNotCalled<ControllerWithControllerContextParam>()
    }

    @Test
    fun testControllerContextNotAccessibleAfterHandler() {
        scenario<ControllerContextHolder> {
            assertThat {
                rootNotCalled()
                assertNull(ControllerContextHolder.usedContext)
            }

            user {
                sendTextMessage("pupa")
            }

            assertThat {
                wasCalled<ControllerContextHolder>(1)
                assertNotNull(ControllerContextHolder.usedContext)

                val ex = assertThrows<IllegalStateException> { ControllerContextHolder.usedContext!!.currentController() }

                assertEquals("Context not initialized", ex.message)
            }
        }
    }
}

internal class ControllerUsesControllerContext : BaseController() {
    @TextHandler
    fun handle(msg: String) {
        context.createPage(
            Page(
                message = "Choose menu:",
                contentType = ContentType.Plain,
                messageType = MessageType.Text,
                buttons = listOf(listOf(Button.of<AnotherController>(titleId = "commandId1")))
            )
        )
    }
}

internal class AnotherController : BaseController() {
    @TextHandler
    fun handle(message: String) {
    }
}

internal class ControllerWithControllerContextParam : BaseController() {
    /**
     * Controller cannot use ControllerContext as handler's parameter
     */
    @TextHandler
    fun handle(msg: String, context: ControllerContext) {
        CODE_NOT_REACHED()
    }
}

internal class ControllerContextHolder : BaseController() {
    @TextHandler
    fun handle(msg: String) {
        usedContext = context
    }

    companion object {
        var usedContext: ControllerContext? = null
    }
}

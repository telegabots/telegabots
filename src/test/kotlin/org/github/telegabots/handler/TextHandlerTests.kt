package org.github.telegabots.handler

import org.github.telegabots.BaseTests
import org.github.telegabots.CODE_NOT_REACHED
import org.github.telegabots.api.BaseController
import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler
import org.github.telegabots.error.ControllerInvokeException
import org.github.telegabots.test.scenario
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.text.ParseException
import kotlin.test.assertEquals

/**
 * Tests related with annotation TextHandler
 */
class TextHandlerTests : BaseTests() {
    @Test
    fun testController_Fail_WhenControllerWithoutAnyHandler() {
        val ex = assertThrows<IllegalStateException> {
            scenario<InvalidRootControllerWithoutTextHandler> { }
        }

        assertEquals(
            "Root controller (org.github.telegabots.handler.InvalidRootControllerWithoutTextHandler) have to implement text handler. Annotate method with @TextHandler",
            ex.message
        )
    }

    @Test
    fun testController_Fail_WhenTextHandlerHasNotStringParam() {
        val ex = assertThrows<IllegalStateException> {
            scenario<InvalidControllerWithoutStringParam> { }
        }

        assertEquals(
            "First parameter must be String but found int in handler public final void org.github.telegabots.handler.InvalidControllerWithoutStringParam.execute(int)",
            ex.message
        )
    }

    @Test
    fun testController_Success_WhenHandlerReturnsVoid() {
        scenario<SimpleControllerReturnsVoid> {
            assertThat {
                rootNotCalled()
            }

            user {
                sendTextMessage("Make Kotlin GA!")
            }

            assertThat {
                rootWasCalled(1)
                controllerReturnTrue()
            }
        }
    }

    @Test
    fun testController_Success_WhenHandlerReturnsBool() {
        scenario<SimpleControllerReturnsBool> {
            assertThat {
                rootNotCalled()
            }

            user {
                sendTextMessage("Make Kotlin Great!")
            }

            assertThat {
                rootWasCalled(1)
                controllerReturnFalse()
            }
        }
    }

    @Test
    fun testController_Success_WhenHandlerInherited() {
        scenario<InheritSimpleController> {
            assertThat {
                rootNotCalled()
            }

            user {
                sendTextMessage("Make Java Great!")
            }

            assertThat {
                rootWasCalled(1)
                controllerReturnFalse()
            }
        }
    }

    @Test
    fun testController_Fail_WhenHandlerWithoutParams() {
        val ex = assertThrows<IllegalStateException> {
            scenario<InvalidControllerWithoutAnyParam> { }
        }

        assertEquals(
            "Handler must contains at least one parameter: public final boolean org.github.telegabots.handler.InvalidControllerWithoutAnyParam.execute()",
            ex.message
        )
    }

    @Test
    fun testController_Fail_WhenHandlerReturnsNonBool() {
        val ex = assertThrows<IllegalStateException> {
            scenario<InvalidControllerReturnNonBoolParam> { }
        }

        assertEquals(
            "Handler must return bool or void but it returns int in method public final int org.github.telegabots.handler.InvalidControllerReturnNonBoolParam.execute(java.lang.String)",
            ex.message
        )
    }

    @Test
    fun testController_WhenHandlerThrowsError() {
        scenario<SimpleControllerThrowsError> {
            assertThat { rootNotCalled() }

            user {
                val ex = assertThrows<ControllerInvokeException> { sendTextMessage("!?!") }

                assertEquals(ParseException::class.java, ex.cause!!::class.java)
                assertEquals("Controller must throw error", ex.cause?.message)
                assertEquals(SimpleControllerThrowsError::class.java, ex.controller)
            }

            assertThat { rootNotCalled() }
        }
    }
}

internal class InvalidRootControllerWithoutTextHandler : BaseController() {
    @InlineHandler
    fun handle(msg: String, messageId: Int) {

    }
}

internal class InvalidControllerWithoutStringParam : BaseController() {
    @TextHandler
    fun execute(mustBeString: Int) {
        CODE_NOT_REACHED()
    }
}

internal class InvalidControllerWithoutAnyParam : BaseController() {
    @TextHandler
    fun execute(): Boolean {
        CODE_NOT_REACHED()
    }
}

internal class InvalidControllerReturnNonBoolParam : BaseController() {
    @TextHandler
    fun execute(text: String): Int {
        CODE_NOT_REACHED()
    }
}

internal class SimpleControllerThrowsError : BaseController() {
    @TextHandler
    fun execute(text: String): Nothing {
        throw ParseException("Controller must throw error", 0)
    }
}

internal class SimpleControllerReturnsVoid : BaseController() {
    @TextHandler
    fun execute(text: String) {
    }
}

internal open class SimpleControllerReturnsBool : BaseController() {
    @TextHandler
    fun execute(text: String): Boolean = false
}

internal class InheritSimpleController : SimpleControllerReturnsBool()

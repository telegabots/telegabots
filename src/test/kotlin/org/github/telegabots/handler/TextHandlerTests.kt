package org.github.telegabots.handler

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.BaseTests
import org.github.telegabots.CODE_NOT_REACHED
import org.github.telegabots.api.InputMessage
import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler
import org.github.telegabots.error.CommandInvokeException
import org.github.telegabots.test.CommandAssert.assertNotCalled
import org.github.telegabots.test.CommandAssert.assertWasCalled
import org.github.telegabots.test.scenario
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.text.ParseException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Tests related with annotation TextHandler
 */
class TextHandlerTests : BaseTests() {
    @Test
    fun testCommand_Fail_WhenCommandWithoutAnyHandler() {
        val ex = assertThrows<IllegalStateException> {
            scenario<InvalidRootCommandWithoutTextHandler> { }
        }

        assertEquals(
            "Root command (org.github.telegabots.handler.InvalidRootCommandWithoutTextHandler) have to implement text handler. Annotate method with @TextHandler",
            ex.message
        )
    }

    @Test
    fun testCommand_Fail_WhenTextHandlerHasNotStringParam() {
        val ex = assertThrows<IllegalStateException> {
            scenario<InvalidCommandWithoutStringParam> { }
        }

        assertEquals(
            "First parameter must be String but found int in handler public final void org.github.telegabots.handler.InvalidCommandWithoutStringParam.execute(int)",
            ex.message
        )
    }

    @Test
    fun testCommand_Success_WhenHandlerNotReturnsBool() {
        val executor = createExecutor(SimpleCommandWithoutBoolReturn::class.java)
        val update = createAnyTextMessage()

        assertNotCalled<SimpleCommandWithoutBoolReturn>()

        val success = executor.handle(update)

        assertWasCalled<SimpleCommandWithoutBoolReturn>()
        assertTrue(success)
    }

    @Test
    fun testCommand_Success_WhenHandlerReturnsBool() {
        val executor = createExecutor(SimpleCommandReturnsBool::class.java)
        val update = createAnyTextMessage()

        assertNotCalled<SimpleCommandReturnsBool>()

        val success = executor.handle(update)

        assertWasCalled<SimpleCommandReturnsBool>()
        assertFalse(success)
    }

    @Test
    fun testCommand_Success_WhenHandlerInherited() {
        val executor = createExecutor(InheritSimpleCommand::class.java)
        val update = createAnyTextMessage()

        assertNotCalled<InheritSimpleCommand>()

        val success = executor.handle(update)

        assertWasCalled<InheritSimpleCommand>()
        assertFalse(success)
    }

    @Test
    fun testCommand_Fail_WhenHandlerWithoutParams() {
        val ex = assertThrows<IllegalStateException> {
            scenario<InvalidCommandWithoutAnyParam> { }
        }

        assertEquals(
            "Handler must contains at least one parameter: public final boolean org.github.telegabots.handler.InvalidCommandWithoutAnyParam.execute()",
            ex.message
        )
    }

    @Test
    fun testCommand_Fail_WhenHandlerReturnsNonBool() {
        val ex = assertThrows<IllegalStateException> {
            scenario<InvalidCommandReturnNonBoolParam> { }
        }

        assertEquals(
            "Handler must return bool or void but it returns int in method public final int org.github.telegabots.handler.InvalidCommandReturnNonBoolParam.execute(java.lang.String)",
            ex.message
        )
    }

    @Test
    fun testCommand_WhenHandlerThrowsError() {
        val executor = createExecutor(SimpleCommandThrowsError::class.java)
        val update = createAnyTextMessage()
        val ex = assertThrows<CommandInvokeException> { executor.handle(update) }

        assertEquals(ParseException::class.java, ex.cause!!::class.java)
        assertEquals("Command must throw error", ex.cause?.message)
        assertEquals(SimpleCommandThrowsError::class.java, ex.command)
    }
}

internal class InvalidRootCommandWithoutTextHandler : BaseCommand() {
    @InlineHandler
    fun handle(msg: String, messageId: Int) {

    }
}

internal class InvalidCommandWithoutStringParam : BaseCommand() {
    @TextHandler
    fun execute(mustBeString: Int) {
        CODE_NOT_REACHED()
    }
}

internal class InvalidCommandWithoutAnyParam : BaseCommand() {
    @TextHandler
    fun execute(): Boolean {
        CODE_NOT_REACHED()
    }
}

internal class InvalidCommandReturnNonBoolParam : BaseCommand() {
    @TextHandler
    fun execute(text: String): Int {
        CODE_NOT_REACHED()
    }
}

internal class SimpleCommandThrowsError : BaseCommand() {
    @TextHandler
    fun execute(text: String): Nothing {
        throw ParseException("Command must throw error", 0)
    }
}

internal class SimpleCommandWithoutBoolReturn : BaseCommand() {
    @TextHandler
    fun execute(text: String) {
    }
}

internal open class SimpleCommandReturnsBool : BaseCommand() {
    @TextHandler
    fun execute(text: String): Boolean = false
}

internal class InheritSimpleCommand : SimpleCommandReturnsBool()

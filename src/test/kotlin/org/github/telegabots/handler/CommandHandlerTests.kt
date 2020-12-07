package org.github.telegabots.handler

import org.github.telegabots.BaseCommand
import org.github.telegabots.BaseTests
import org.github.telegabots.CODE_NOT_REACHED
import org.github.telegabots.annotation.CommandHandler
import org.github.telegabots.error.CommandInvokeException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.text.ParseException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests related with annotation CommandHandler
 */
class CommandHandlerTests : BaseTests() {
    @Test
    fun testCommand_Fail_WhenTextHandlerHasNotStringParam() {
        val executor = createExecutor(InvalidCommandWithoutStringParam::class.java)
        val update = createAnyMessage()
        val ex = assertThrows<IllegalStateException> { executor.handle(update) }

        assertEquals(
            "First parameter must be String but found int in handler public final void org.github.telegabots.handler.InvalidCommandWithoutStringParam.execute(int)",
            ex.message
        )
    }

    @Test
    fun testCommand_Success_WhenHandlerNotReturnsBool() {
        val executor = createExecutor(SimpleCommandWithoutBoolReturn::class.java)
        val update = createAnyMessage()
        val success = executor.handle(update)

        assertTrue(success)
    }

    @Test
    fun testCommand_Success_WhenHandlerReturnsBool() {
        val executor = createExecutor(SimpleCommandReturnsBool::class.java)
        val update = createAnyMessage()
        val success = executor.handle(update)

        assertFalse(success)
    }

    @Test
    fun testCommand_Success_WhenHandlerInherited() {
        val executor = createExecutor(InheritSimpleCommand::class.java)
        val update = createAnyMessage()
        val success = executor.handle(update)

        assertFalse(success)
    }

    @Test
    fun testCommand_Fail_WhenHandlerWithoutParams() {
        val executor = createExecutor(InvalidCommandWithoutAnyParam::class.java)
        val update = createAnyMessage()
        val ex = assertThrows<IllegalStateException> { executor.handle(update) }

        assertEquals(
            "Handler must contains at least one parameter: public final boolean org.github.telegabots.handler.InvalidCommandWithoutAnyParam.execute()",
            ex.message
        )
    }

    @Test
    fun testCommand_Fail_WhenHandlerReturnsNonBool() {
        val executor = createExecutor(InvalidCommandReturnNonBoolParam::class.java)
        val update = createAnyMessage()
        val ex = assertThrows<IllegalStateException> { executor.handle(update) }

        assertEquals(
            "Handler must return bool or void but it returns int in method public final int org.github.telegabots.handler.InvalidCommandReturnNonBoolParam.execute(java.lang.String)",
            ex.message
        )
    }

    @Test
    fun testCommand_WhenHandlerThrowsError() {
        val executor = createExecutor(SimpleCommandThrowsError::class.java)
        val update = createAnyMessage()
        val ex = assertThrows<CommandInvokeException> { executor.handle(update) }

        assertEquals(ParseException::class.java, ex.cause!!::class.java)
        assertEquals("Command must throw error", ex.cause?.message)
        assertEquals(SimpleCommandThrowsError::class.java, ex.command)
    }
}

internal class InvalidCommandWithoutStringParam : BaseCommand() {
    @CommandHandler
    fun execute(mustBeString: Int) {
        CODE_NOT_REACHED()
    }
}

internal class InvalidCommandWithoutAnyParam : BaseCommand() {
    @CommandHandler
    fun execute(): Boolean {
        CODE_NOT_REACHED()
    }
}

internal class InvalidCommandReturnNonBoolParam : BaseCommand() {
    @CommandHandler
    fun execute(text: String): Int {
        CODE_NOT_REACHED()
    }
}

internal class SimpleCommandThrowsError : BaseCommand() {
    @CommandHandler
    fun execute(text: String): Nothing {
        throw ParseException("Command must throw error", 0)
    }
}

internal class SimpleCommandWithoutBoolReturn : BaseCommand() {
    @CommandHandler
    fun execute(text: String) {
    }
}

internal open class SimpleCommandReturnsBool : BaseCommand() {
    @CommandHandler
    fun execute(text: String): Boolean = false
}

internal class InheritSimpleCommand : SimpleCommandReturnsBool()

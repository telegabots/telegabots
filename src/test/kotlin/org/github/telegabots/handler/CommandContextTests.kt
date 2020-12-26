package org.github.telegabots.handler

import org.github.telegabots.*
import org.github.telegabots.api.annotation.TextHandler
import org.github.telegabots.api.*
import org.github.telegabots.error.CommandInvokeException
import org.github.telegabots.test.CommandAssert.assertNotCalled
import org.github.telegabots.test.CommandAssert.assertWasCalled
import org.github.telegabots.test.CommandAssert.resetCalled
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CommandContextTests : BaseTests() {
    companion object {
        const val USER_ID: Int = 55998866
    }

    @Test
    fun testWhenInnerCommandInvoked() {
        val executor = createExecutor(CommandUsesCommandContext::class.java)
        val update1 = createAnyTextMessage(userId = USER_ID)
        executor.addLocalization(USER_ID, "commandId1" to "Command Id Title")

        resetCalled<CommandUsesCommandContext>()

        val success1 = executor.handle(update1)

        assertWasCalled<CommandUsesCommandContext>()
        assertTrue(success1)

        val update2 = createAnyTextMessage(messageText = "Command Id Title", userId = USER_ID)

        assertNotCalled<AnotherCommand>()

        val success2 = executor.handle(update2)

        assertWasCalled<CommandUsesCommandContext>(1)
        assertTrue(success2)
        assertWasCalled<AnotherCommand>()
    }

    @Test
    fun testWhenInnerCommandNotInvoked() {
        val executor = createExecutor(CommandUsesCommandContext::class.java)
        val update1 = createAnyTextMessage(userId = USER_ID)
        executor.addLocalization(USER_ID, "commandId1" to "Command Id Title")

        resetCalled<CommandUsesCommandContext>()

        val success1 = executor.handle(update1)

        assertWasCalled<CommandUsesCommandContext>()
        assertTrue(success1)

        val update2 = createAnyTextMessage(messageText = "Command Id Title 2", userId = USER_ID)

        assertNotCalled<AnotherCommand>()

        val success2 = executor.handle(update2)

        assertWasCalled<CommandUsesCommandContext>(2)
        assertTrue(success2)
        assertNotCalled<AnotherCommand>()
    }

    @Test
    fun testCommand_Fail_WhenHandlerUseCommandContextAsParam() {
        val executor = createExecutor(CommandWithCommandContextParam::class.java)
        val update = createAnyTextMessage()

        assertNotCalled<CommandWithCommandContextParam>()

        val ex = assertThrows<CommandInvokeException> { executor.handle(update) }

        assertEquals(
            "CommandContext can not be used as handler parameter. Use \"context\" field instead",
            ex.cause!!.message
        )
        assertEquals(IllegalStateException::class.java, ex.cause!!::class.java)
        assertEquals(CommandWithCommandContextParam::class.java, ex.command)
        assertNotCalled<CommandWithCommandContextParam>()
    }

    @Test
    fun testCommandContextNotAccessibleAfterHandler() {
        val executor = createExecutor(CommandContextHolder::class.java)
        val update1 = createAnyTextMessage()

        assertNotCalled<CommandContextHolder>()
        assertNull(CommandContextHolder.usedContext)

        executor.handle(update1)

        assertWasCalled<CommandContextHolder>()
        assertNotNull(CommandContextHolder.usedContext)

        val ex = assertThrows<IllegalStateException> { CommandContextHolder.usedContext!!.currentCommand() }

        assertEquals("Command context not initialized for current command", ex.message)
    }
}

internal class CommandUsesCommandContext : BaseCommand() {
    @TextHandler
    fun handle(msg: String) {
        context.createPage(
            Page(
                message = "Choose menu:",
                contentType = ContentType.Plain,
                messageType = MessageType.Text,
                subCommands = listOf(listOf(SubCommand.of<AnotherCommand>(titleId = "commandId1")))
            )
        )
    }
}

internal class AnotherCommand : BaseCommand() {
    @TextHandler
    fun handle(message: String) {
    }
}

internal class CommandWithCommandContextParam : BaseCommand() {
    /**
     * Command can not use CommandContext as handler's parameter
     */
    @TextHandler
    fun handle(msg: String, context: CommandContext) {
        CODE_NOT_REACHED()
    }
}

internal class CommandContextHolder : BaseCommand() {
    @TextHandler
    fun handle(msg: String) {
        usedContext = context
    }

    companion object {
        var usedContext: CommandContext? = null
    }
}

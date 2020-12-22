package org.github.telegabots.handler

import org.github.telegabots.*
import org.github.telegabots.annotation.CommandHandler
import org.github.telegabots.error.CommandInvokeException
import org.github.telegabots.test.CommandAssert.assertNotCalled
import org.github.telegabots.test.CommandAssert.assertWasCalled
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class CommandContextTests : BaseTests() {
    companion object {
        const val USER_ID: Int = 55998866
    }

    @Test
    fun testInnerCommand() {
        val executor = createExecutor(CommandUsesCommandContext::class.java)
        val update1 = createAnyMessage(userId = USER_ID)
        executor.addLocalization(USER_ID, "commandId1" to "Command Id Title")

        assertNotCalled<CommandUsesCommandContext>()

        val success1 = executor.handle(update1)

        assertWasCalled<CommandUsesCommandContext>()
        assertTrue(success1)

        val update2 = createAnyMessage(messageText = "Command Id Title", userId = USER_ID)

        assertNotCalled<AnotherCommand>()

        val success2 = executor.handle(update2)

        assertTrue(success2)
        assertWasCalled<AnotherCommand>()
    }

    @Test
    fun testCommand_Fail_WhenHandlerUseCommandContextAsParam() {
        val executor = createExecutor(CommandWithCommandContextParam::class.java)
        val update = createAnyMessage()

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
}

internal class CommandUsesCommandContext : BaseCommand() {
    @CommandHandler
    fun handle(msg: String) {
        context.sendMessage(
            "Choose menu:",
            contentType = ContentType.Plain,
            messageType = MessageType.Callback,
            subCommands = listOf(listOf(SubCommand.of<AnotherCommand>(titleId = "commandId1")))
        )
    }
}

internal class AnotherCommand : BaseCommand() {
    @CommandHandler
    fun handle(message: String) {
    }
}

internal class CommandWithCommandContextParam : BaseCommand() {
    /**
     * Command can not use CommandContext as handler's parameter
     */
    @CommandHandler
    fun handle(msg: String, context: CommandContext) {
        CODE_NOT_REACHED()
    }
}

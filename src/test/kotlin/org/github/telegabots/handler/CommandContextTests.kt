package org.github.telegabots.handler

import org.github.telegabots.*
import org.github.telegabots.annotation.CommandHandler
import org.github.telegabots.test.CommandAssert.assertNotCalled
import org.github.telegabots.test.CommandAssert.assertWasCalled
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommandContextTests : BaseTests() {
    companion object {
        const val USER_ID: Int = 55998866
    }

    @Test
    fun testInnerCommand() {
        val executor = createExecutor(CommandWithCommandContext::class.java)
        val update1 = createAnyMessage(userId = USER_ID)
        executor.addLocalization(USER_ID, "commandId1" to "Command Id Title")

        assertNotCalled<CommandWithCommandContext>()

        val success1 = executor.handle(update1)

        assertWasCalled<CommandWithCommandContext>()
        assertTrue(success1)

        val update2 = createAnyMessage(messageText = "Command Id Title", userId = USER_ID)

        assertNotCalled<AnotherCommand>()

        val success2 = executor.handle(update2)

        assertTrue(success2)
        assertWasCalled<AnotherCommand>()
    }
}

internal class CommandWithCommandContext : BaseCommand() {
    @CommandHandler
    fun handle(msg: String, context: CommandContext) {
        context.sendMessage(
            "Choose menu:",
            contentType = ContentType.Plain,
            messageType = MessageType.CALLBACK,
            subCommands = listOf(listOf(SubCommand("commandId1", handler = AnotherCommand::class.java)))
        )
    }
}

internal class AnotherCommand : BaseCommand() {
    @CommandHandler
    fun handle(message: String) {
    }
}

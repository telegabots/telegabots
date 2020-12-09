package org.github.telegabots.handler

import org.github.telegabots.*
import org.github.telegabots.annotation.CommandHandler
import org.github.telegabots.test.assertNotCalled
import org.github.telegabots.test.assertWasCalled
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommandContextTests : BaseTests() {
    @Test
    fun testInnerCommand() {
        val executor = createExecutor(CommandWithCommandContext::class.java)
        val update1 = createAnyMessage()

        CommandWithCommandContext::class.assertNotCalled()

        val success1 = executor.handle(update1)

        CommandWithCommandContext::class.assertWasCalled()
        assertTrue(success1)

        val update2 = createAnyMessage("Command Id Title")

        AnotherCommand::class.assertNotCalled()

        val success2 = executor.handle(update2)

        assertTrue(success2)

        AnotherCommand::class.assertWasCalled()
    }
}

internal class CommandWithCommandContext : BaseCommand() {
    @CommandHandler
    fun handle(msg: String, context: CommandContext) {
        context.sendMessage(
            "Choose menu:",
            contentType = ContentType.Simple,
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

package org.github.telegabots.handler

import org.github.telegabots.*
import org.github.telegabots.annotation.CommandHandler
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class CommandContextTests : BaseTests() {
    @Test
    fun testInnerCommand() {
        val executor = createExecutor(CommandWithCommandContext::class.java)
        val update = createAnyMessage()
        val success = executor.handle(update)

        assertTrue(success)
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

package org.github.telegabots.handler

import org.github.telegabots.*
import org.github.telegabots.annotation.CallbackHandler
import org.github.telegabots.annotation.CommandHandler
import org.github.telegabots.test.CommandAssert.assertNotCalled
import org.github.telegabots.test.CommandAssert.assertWasCalled
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MultiSubCommandTests : BaseTests() {
    @Test
    fun testMultipleSubcommands() {
        val userId = nextRandomInt()
        val executor = createExecutor(CommandRoot::class.java)
        val update1 = createAnyMessage(userId = userId)

        assertNotCalled<CommandRoot>()
        assertNull(CommandRoot.lastMessageId)

        val success1 = executor.handle(update1)

        assertWasCalled<CommandRoot>()
        assertTrue(success1)

        //--------
        val update2 = createAnyMessage(messageText = "SUB_MENU1", userId = userId)

        assertNotCalled<SubMenu1Command>()
        assertNotNull(CommandRoot.lastMessageId)
        val messageId = CommandRoot.lastMessageId!!

        val success2 = executor.handle(update2)

        assertWasCalled<CommandRoot>(2)
        assertTrue(success2)
        assertNotCalled<SubMenu1Command>()
        assertEquals(messageId, CommandRoot.lastMessageId)

        //--------
        val update3 = createAnyCallbackMessage(messageId = messageId, callbackData = "SUB_MENU1", userId = userId)

        assertNotCalled<SubMenu1Command>()

        val success3 = executor.handle(update3)

        assertWasCalled<CommandRoot>(2)
        assertTrue(success3)
        assertWasCalled<SubMenu1Command>()

        //--------
        val update4 = createAnyCallbackMessage(messageId = messageId, callbackData = "SUB_MENU2", userId = userId)

        assertNotCalled<SubMenu2Command>()

        val success4 = executor.handle(update4)

        assertWasCalled<CommandRoot>(2)
        assertTrue(success4)
        assertWasCalled<SubMenu2Command>()
    }
}

internal class CommandRoot : BaseCommand() {
    @CommandHandler
    fun handle(msg: String) {
        if (lastMessageId == null) {
            lastMessageId = context.sendMessage(
                "Choose menu:",
                contentType = ContentType.Plain,
                messageType = MessageType.Callback,
                subCommands = listOf(listOf(SubCommand.of<SubMenu1Command>(), SubCommand.of<SubMenu2Command>()))
            )
        }
    }

    companion object {
        var lastMessageId: Int? = null
    }
}

internal class SubMenu1Command : BaseCommand() {
    @CallbackHandler
    fun handle(message: String, messageId: Int) {
    }
}

internal class SubMenu2Command : BaseCommand() {
    @CallbackHandler
    fun handle(message: String, messageId: Int) {
    }
}

package org.github.telegabots.handler

import org.github.telegabots.BaseTests
import org.github.telegabots.CODE_NOT_REACHED
import org.github.telegabots.api.*
import org.github.telegabots.api.annotation.TextHandler
import org.github.telegabots.test.CommandAssert.assertNotCalled
import org.github.telegabots.test.CommandAssert.resetCalled
import org.github.telegabots.test.scenario
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CommandContextTests : BaseTests() {
    @Test
    fun testWhenInnerCommandInvoked() {
        scenario<CommandUsesCommandContext> {
            user {
                resetCalled<CommandUsesCommandContext>()
                addLocalization("commandId1" to "Command Id Title")
                sendTextMessage("foo bar 2")
            }

            assertThat {
                wasCalled<CommandUsesCommandContext>(1)
                notCalled<AnotherCommand>()
            }

            user {
                sendTextMessage("Command Id Title")
            }

            assertThat {
                wasCalled<CommandUsesCommandContext>(1)
                wasCalled<AnotherCommand>(1)
            }
        }
    }

    @Test
    fun testWhenInnerCommandNotInvoked() {
        scenario<CommandUsesCommandContext> {
            user {
                resetCalled<CommandUsesCommandContext>()
                addLocalization("commandId1" to "Command Id Title")
                sendTextMessage("foo bar 2")
            }

            assertThat {
                wasCalled<CommandUsesCommandContext>(1)
                notCalled<AnotherCommand>()
            }

            user {
                sendTextMessage("Command Id Title 2")
            }

            assertThat {
                wasCalled<CommandUsesCommandContext>(2)
                notCalled<AnotherCommand>()
            }
        }
    }

    @Test
    fun testCommand_Fail_WhenHandlerUseCommandContextAsParam() {
        val ex = assertThrows<IllegalStateException> { scenario<CommandWithCommandContextParam> {}}

        assertEquals(
            "CommandContext can not be used as handler parameter. Use \"context\" field instead. Handler: public final void org.github.telegabots.handler.CommandWithCommandContextParam.handle(java.lang.String,org.github.telegabots.api.CommandContext)",
            ex.message
        )
        assertNotCalled<CommandWithCommandContextParam>()
    }

    @Test
    fun testCommandContextNotAccessibleAfterHandler() {
        scenario<CommandContextHolder> {
            assertThat {
                rootNotCalled()
                assertNull(CommandContextHolder.usedContext)
            }

            user {
                sendTextMessage("pupa")
            }

            assertThat {
                wasCalled<CommandContextHolder>(1)
                assertNotNull(CommandContextHolder.usedContext)

                val ex = assertThrows<IllegalStateException> { CommandContextHolder.usedContext!!.currentCommand() }

                assertEquals("Context not initialized", ex.message)
            }
        }
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

package org.github.telegabots.util

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.SubCommand
import org.github.telegabots.state.State
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SubCommandTests {
    @Test
    fun testTitleIdByCommandClass() {
        assertEquals("FOO_BAR", SubCommand.titleIdOf(FooBarCommand::class.java))
    }

    @Test
    fun testTitleIdByCommandWithDigits() {
        assertEquals("FOO_BAR_COMMAND1", SubCommand.titleIdOf(FooBarCommand1::class.java))
        assertEquals("FOO_BAR1", SubCommand.titleIdOf(FooBar1Command::class.java))
    }

    @Test
    fun testCreateSubCommandByCommandClass() {
        val expected = SubCommand(handler = FooBarCommand::class.java, titleId = "FOO_BAR", state = null)

        assertEquals(expected, SubCommand.of(FooBarCommand::class.java))
        assertEquals(expected, SubCommand.of(FooBarCommand::class.java, titleId = "FOO_BAR"))
        assertEquals(expected, SubCommand.of<FooBarCommand>())
        assertEquals(expected, SubCommand.of<FooBarCommand>(titleId = "FOO_BAR"))
    }

    @Test
    fun testCreateSubCommandByCommandClassWithEmptyState() {
        val expected = SubCommand(handler = FooBarCommand::class.java, titleId = "FOO_BAR", state = State.Empty)

        assertEquals(expected, SubCommand.of(FooBarCommand::class.java, State.Empty))
        assertEquals(expected, SubCommand.of(FooBarCommand::class.java, titleId = "FOO_BAR", state = State.Empty))
        assertEquals(expected, SubCommand.of<FooBarCommand>(state = State.Empty))
        assertEquals(expected, SubCommand.of<FooBarCommand>(titleId = "FOO_BAR", state = State.Empty))
    }

    @Test
    fun testCreateSubCommandByCommandClassAndCustomerTitleId() {
        val expected = SubCommand(handler = FooBarCommand::class.java, titleId = "SOME_ANOTHER_ID", state = null)

        assertEquals(expected, SubCommand.of(FooBarCommand::class.java, titleId = "SOME_ANOTHER_ID"))
    }
}

internal class FooBarCommand : BaseCommand()

internal class FooBarCommand1 : BaseCommand()

internal class FooBar1Command : BaseCommand()

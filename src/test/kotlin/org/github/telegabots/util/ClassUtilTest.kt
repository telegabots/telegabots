package org.github.telegabots.util

import org.junit.jupiter.api.Test
import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.State
import org.github.telegabots.api.annotation.CommandHandler
import java.io.Serializable
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ClassUtilTest {
    @Test
    fun testGetHandlers() {
        val command = TestCommand()
        val handlers = CommandClassUtil.getHandlers(command)

        handlers.forEach { handler ->
            println("\t${handler.name}")
            handler.params.forEach {
                println("\t\t${it.type} (${it.innerType})")
            }
        }

        assertEquals(1, handlers.size)
        val handler = handlers[0]
        assertEquals("handleCommand", handler.name)
        assertNotNull(handler.method)
        assertEquals(3, handler.params.size)
        val params = handler.params
        assertEquals(String::class.java, params[0].type)
        assertEquals(null, params[0].innerType)
        assertEquals(State::class.java, params[1].type)
        assertEquals(String::class.java, params[1].innerType)
        assertEquals(State::class.java, params[2].type)
        assertEquals(TestDto::class.java, params[2].innerType)
    }
}

class TestCommand : BaseCommand() {
    @CommandHandler
    fun handleCommand(
        text: String,
        stateStr: State<String>,
        stateDto: State<TestDto>
    ) {

    }
}

class TestDto(var str: String, var int: Int) : Serializable

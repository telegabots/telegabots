package org.github.telegabots.handler

import org.github.telegabots.BaseCommand
import org.github.telegabots.BaseTests
import org.github.telegabots.Service
import org.github.telegabots.annotation.CommandHandler
import org.github.telegabots.error.CommandInvokeException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServiceParamTests : BaseTests() {
    @Test
    fun testServiceCall_Success_WhenServiceRegistered() {
        val executor = createExecutor(CommandWithServiceParam::class.java)
        val update = createAnyMessage(messageText = "Hello from client!")
        executor.addService(SimpleTestService::class.java, SimpleTestService())

        assertFalse(CommandWithServiceParam.called.get())

        executor.handle(update)

        assertTrue(CommandWithServiceParam.called.get())
    }

    @Test
    fun testServiceCall_Fail_WhenServiceNotRegistered() {
        val executor = createExecutor(CommandWithServiceParam::class.java)
        val update = createAnyMessage(messageText = "Hello from client!")

        val ex = assertThrows<CommandInvokeException> { executor.handle(update) }

        assertEquals(IllegalStateException::class.java, ex.cause!!::class.java)
        assertEquals(CommandWithServiceParam::class.java, ex.command)
        assertEquals("Service not found: org.github.telegabots.handler.SimpleTestService", ex.cause?.message)
    }
}

internal class CommandWithServiceParam : BaseCommand() {
    @CommandHandler
    fun handle(msg: String, service: SimpleTestService) {
        assertEquals("Hello from client!", msg)
        assertEquals("Hello from service, Ruslan", service.greet("Ruslan"))

        called.set(true)
    }

    companion object {
        val called = AtomicBoolean()
    }
}

internal class SimpleTestService : Service {
    fun greet(name: String): String = "Hello from service, $name"
}

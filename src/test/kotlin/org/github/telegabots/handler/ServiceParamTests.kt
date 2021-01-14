package org.github.telegabots.handler

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.BaseTests
import org.github.telegabots.api.Service
import org.github.telegabots.api.annotation.TextHandler
import org.github.telegabots.error.CommandInvokeException
import org.github.telegabots.test.scenario
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ServiceParamTests : BaseTests() {
    @Test
    fun testServiceCall_Success_WhenServiceRegistered() {
        scenario<CommandWithServiceParam> {
            addService(SimpleTestService::class.java, SimpleTestService())
            resetRootCall()

            assertThat {
                assertFalse(SimpleTestService.calledWith("Ruslan"))
            }

            user {
                sendTextMessage("Hello from client!")
            }

            assertThat {
                rootWasCalled(1)
                assertTrue(SimpleTestService.calledWith("Ruslan"))
            }
        }
    }

    @Test
    fun testServiceCall_Fail_WhenServiceNotRegistered() {
        scenario<CommandWithServiceParam> {
            resetRootCall()

            user {
                val ex = assertThrows<CommandInvokeException> { sendTextMessage("Hello from client!") }

                assertEquals(IllegalStateException::class.java, ex.cause!!::class.java)
                assertEquals(CommandWithServiceParam::class.java, ex.command)
                assertEquals("Service not found: org.github.telegabots.handler.SimpleTestService", ex.cause?.message)
            }

            assertThat {
                rootNotCalled()
            }
        }
    }
}

internal class CommandWithServiceParam : BaseCommand() {
    @TextHandler
    fun handle(msg: String, service: SimpleTestService) {
        assertEquals("Hello from client!", msg)
        assertEquals("Hello from service, Ruslan", service.greet("Ruslan"))
    }
}

internal class SimpleTestService : Service {
    fun greet(name: String): String {
        calledNames.add(name)

        return "Hello from service, $name"
    }

    companion object {
        private val calledNames = mutableListOf<String>()

        fun calledWith(name: String) = calledNames.contains(name)
    }
}

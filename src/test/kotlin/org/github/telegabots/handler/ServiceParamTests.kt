package org.github.telegabots.handler

import org.github.telegabots.api.BaseController
import org.github.telegabots.BaseTests
import org.github.telegabots.api.Service
import org.github.telegabots.api.annotation.TextHandler
import org.github.telegabots.error.ControllerInvokeException
import org.github.telegabots.test.scenario
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ServiceParamTests : BaseTests() {
    @Test
    fun testServiceCall_Success_WhenServiceRegistered() {
        scenario<ControllerWithServiceParam> {
            addService(SimpleTestService::class.java, SimpleTestService())

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
        scenario<ControllerWithServiceParam> {

            user {
                val ex = assertThrows<ControllerInvokeException> { sendTextMessage("Hello from client!") }

                assertEquals(IllegalStateException::class.java, ex.cause!!::class.java)
                assertEquals(ControllerWithServiceParam::class.java, ex.controller)
                assertEquals("Service not found: org.github.telegabots.handler.SimpleTestService", ex.cause?.message)
            }

            assertThat {
                rootNotCalled()
            }
        }
    }

    @Test
    fun testServiceCallInCtor_Success_WhenServiceRegistered() {
        scenario<ControllerWithServiceParamInCtor>(listOf(SimpleTestService())) {

            assertThat {
                assertFalse(SimpleTestService.calledWith("Polina"))
            }

            user {
                sendTextMessage("Hello from client2!")
            }

            assertThat {
                rootWasCalled(1)
                assertTrue(SimpleTestService.calledWith("Polina"))
            }
        }
    }

    @Test
    fun testServiceCallInCtor_Success_WhenServiceNotRegistered() {
        val ex = assertThrows<IllegalStateException> {
            scenario<ControllerWithServiceParamInCtor> {
                fail("Should not be called")
            }
        }

        assertEquals("Service not found: org.github.telegabots.handler.SimpleTestService", ex.message)
    }
}

internal class ControllerWithServiceParam : BaseController() {
    @TextHandler
    fun handle(msg: String, service: SimpleTestService) {
        assertEquals("Hello from client!", msg)
        assertEquals("Hello from service, Ruslan", service.greet("Ruslan"))
    }
}

internal class ControllerWithServiceParamInCtor(private val service: SimpleTestService) : BaseController() {
    @TextHandler
    fun handle(msg: String, ) {
        assertEquals("Hello from client2!", msg)
        assertEquals("Hello from service, Polina", service.greet("Polina"))
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

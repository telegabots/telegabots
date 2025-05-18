package org.github.telegabots.test

import org.github.telegabots.api.BaseController
import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import kotlin.reflect.KClass

class StrictExceptionAnswer<T> : Answer<T> {
    override fun answer(invocation: InvocationOnMock): T {
        throw RuntimeException("Mockito strict run failed: $invocation")
    }
}

private val STRICT = StrictExceptionAnswer<Any>()

fun <T> mockStrict(clazz: Class<T>): T {
    return Mockito.mock(clazz, STRICT)
}

private val calledControllers = mutableMapOf<KClass<out BaseController>, Int>()

fun <T : BaseController> KClass<T>.call() {
    synchronized(calledControllers) {
        calledControllers.put(this, (calledControllers[this] ?: 0) + 1)
    }
}

fun <T : BaseController> KClass<T>.called(): Int {
    synchronized(calledControllers) {
        return calledControllers[this] ?: 0
    }
}

fun <T : BaseController> KClass<T>.resetCalled(): Int {
    synchronized(calledControllers) {
        return calledControllers.remove(this) ?: 0
    }
}

fun resetAllCalls() {
    synchronized(calledControllers) {
        calledControllers.clear()
    }
}

fun <T : BaseController> KClass<T>.assertWasCalled(expected: Int = 1) =
    assertEquals(
        expected,
        called()
    ) { "Controller ${this.simpleName} was called ${called()} times but expected $expected" }

fun <T : BaseController> KClass<T>.assertNotCalled() =
    assertEquals(0, called()) { "Controller ${this.simpleName} was called but expected not" }


object ControllerAssert {
    /**
     * Increment controller call counter
     */
    inline fun <reified T : BaseController> call() = T::class.call()

    /**
     * Returns controller's call counter
     */
    inline fun <reified T : BaseController> called(): Int = T::class.called()

    /**
     * Resets controller call counter
     */
    inline fun <reified T : BaseController> resetCalled() = T::class.resetCalled()

    /**
     * Asserts that controller was called expected times
     */
    inline fun <reified T : BaseController> assertWasCalled(expected: Int = 1) = T::class.assertWasCalled(expected)

    /**
     * Assert that controller not called at all
     */
    inline fun <reified T : BaseController> assertNotCalled() = T::class.assertNotCalled()
}

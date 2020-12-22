package org.github.telegabots.test

import org.github.telegabots.BaseCommand
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

private val calledCommands = mutableMapOf<KClass<out BaseCommand>, Int>()

fun <T : BaseCommand> KClass<T>.call() {
    synchronized(calledCommands) {
        calledCommands.put(this, (calledCommands[this] ?: 0) + 1)
    }
}

fun <T : BaseCommand> KClass<T>.called(): Int {
    synchronized(calledCommands) {
        return calledCommands[this] ?: 0
    }
}

fun <T : BaseCommand> KClass<T>.resetCalled(): Int {
    synchronized(calledCommands) {
        return calledCommands.remove(this) ?: 0
    }
}

fun <T : BaseCommand> KClass<T>.assertWasCalled(expected: Int = 1) =
    assertEquals(
        expected,
        called()
    ) { "Command ${this.simpleName} was called ${called()} times but expected $expected" }

fun <T : BaseCommand> KClass<T>.assertNotCalled() =
    assertEquals(0, called()) { "Command ${this.simpleName} was called but expected not" }


object CommandAssert {
    /**
     * Increment command call counter
     */
    inline fun <reified T : BaseCommand> call() = T::class.call()

    /**
     * Returns command's call counter
     */
    inline fun <reified T : BaseCommand> called(): Int = T::class.called()

    /**
     * Resets command call counter
     */
    inline fun <reified T : BaseCommand> resetCalled() = T::class.resetCalled()

    /**
     * Asserts that command was called expected times
     */
    inline fun <reified T : BaseCommand> assertWasCalled(expected: Int = 1) = T::class.assertWasCalled(expected)

    /**
     * Assert that command not called at all
     */
    inline fun <reified T : BaseCommand> assertNotCalled() = T::class.assertNotCalled()
}

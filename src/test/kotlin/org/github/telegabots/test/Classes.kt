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

/**
 * Increment command call counter
 */
fun <T : BaseCommand> KClass<T>.call() {
    synchronized(calledCommands) {
        calledCommands.put(this, (calledCommands[this] ?: 0) + 1)
    }
}

/**
 * Returns command's call counter
 */
fun <T : BaseCommand> KClass<T>.called(): Int {
    synchronized(calledCommands) {
        return calledCommands[this] ?: 0
    }
}

fun <T : BaseCommand> KClass<T>.assertWasCalled(expected: Int = 1) =
    assertEquals(expected, called()) { "Command ${this.simpleName} was called ${called()} times but expected $expected" }

fun <T : BaseCommand> KClass<T>.assertNotCalled() =
    assertEquals(0, called()) { "Command ${this.simpleName} was called but expected not" }

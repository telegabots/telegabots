package org.github.telegabots

import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

class StrictExceptionAnswer<T> : Answer<T> {
    override fun answer(invocation: InvocationOnMock): T {
        throw RuntimeException("Mockito strict run failed: $invocation")
    }
}

private val STRICT = StrictExceptionAnswer<Any>()

fun <T> mockStrict(clazz: Class<T>): T {
    return Mockito.mock(clazz, STRICT)
}

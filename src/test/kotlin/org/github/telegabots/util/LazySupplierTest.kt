package org.github.telegabots.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * Tests for [LazySupplier].
 */
class LazySupplierTest {
    @Test
    fun `get must return the same instance`() {
        val time = LocalDateTime.now()
        val supplier = LazySupplier(time)
        assertEquals(time, supplier.get())
        assertEquals(time, supplier.get())
    }

    @Test
    fun `get must return the same instance even with lazy initializer`() {
        var ready = false
        val supplier = LazySupplier(lazy {
            if (!ready) {
                error("LazySupplier should not call lazy value in constructor")
            }
            LocalDateTime.now()
        })
        ready = true
        val time1 = supplier.get()
        Thread.sleep(2000)
        val time2 = supplier.get()
        assertSame(time1, time2)
    }
}

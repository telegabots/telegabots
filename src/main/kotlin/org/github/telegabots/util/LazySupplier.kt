package org.github.telegabots.util

import java.util.function.Supplier

/**
 * A [Supplier] that can be initialized with a value or a lazy initialization.
 *
 * @param T the type of the value supplied
 * @property valueSupplier the lazy initialization of the value
 */
class LazySupplier<T>(private val valueSupplier: Lazy<T>) : Supplier<T> {
    constructor(value: T) : this(InstantLazy(value))

    override fun get(): T = valueSupplier.value
}

private class InstantLazy<out T>(override val value: T) : Lazy<T> {

    override fun isInitialized(): Boolean = true

    override fun toString(): String = value.toString()
}

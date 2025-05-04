package org.github.telegabots.api

/**
 * Mutable state controller
 */
interface State<T> {
    /**
     * Returns stored value of type T
     */
    fun get(): T?

    /**
     * Sets value and returns previous value
     */
    fun set(value: T?): T?

    /**
     * Return {@code true} if there is a value present, otherwise {@code false}.
     */
    fun isPresent(): Boolean

    /**
     * Return {@code true} if there is no value present, otherwise {@code false}.
     */
    fun isNotPresent(): Boolean = !isPresent()
}

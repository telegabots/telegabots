package org.github.telegabots.state

import org.github.telegabots.api.Service
import org.github.telegabots.api.StateItem
import org.github.telegabots.api.StateKey

/**
 * Common interface for specified kind of state implementation
 */
internal interface StateProvider : Service {
    /**
     * Gets state by state key
     */
    fun get(key: StateKey): StateItem?

    /**
     * Gets state by state key
     */
    fun <T> get(key: StateKey, clazz: Class<T>): T? {
        check(key.type == clazz) { "State type mismatch: ${key.type} != $clazz. Key: $key" }
        return get(key)?.value as T?
    }

    /**
     * Sets state by state key. Returns previous state
     */
    fun set(key: StateKey, value: Any?): StateItem?

    /**
     * Returns all state items
     */
    fun getAll(): List<StateItem>

    /**
     * Merges state items
     */
    fun mergeAll(items: List<StateItem>)

    /**
     * Flushes all state items into permanent storage
     */
    fun flush()

    /**
     * Checks whether it is possible to call flush
     */
    fun canFlush(): Boolean
}
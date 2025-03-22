package org.github.telegabots.state

import org.github.telegabots.api.StateItem
import org.github.telegabots.api.StateKey

/**
 * Facade for all kinds of state implementations
 */
internal interface States {
    /**
     * Gets state by type and name
     */
    fun get(kind: StateKind, key: StateKey): StateItem?

    /**
     * Sets state by type and name. Returns previous state
     */
    fun set(kind: StateKind, key: StateKey, value: Any?): StateItem?

    /**
     * Returns true if state with specified name and type is exists
     */
    fun hasValue(kind: StateKind, key: StateKey): Boolean

    /**
     * Returns all state items
     */
    fun getAll(kind: StateKind): List<StateItem>

    /**
     * Flushes all state items into permanent storage
     */
    fun flush()
}

package org.github.telegabots.state

import org.github.telegabots.api.StateItem
import org.github.telegabots.api.StateKey

/**
 * Facade for all kinds of state implementations
 */
interface States {
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

/**
 * Common interface for specified kind of state implementation
 */
interface StateProvider {
    /**
     * Gets state by state key
     */
    fun get(stateKey: StateKey): StateItem?

    /**
     * Sets state by state key. Returns previous state
     */
    fun set(stateKey: StateKey, value: Any?): StateItem?

    /**
     * Returns all state items
     */
    fun getAll(): List<StateItem>

    /**
     * Flushes all state items into permanent storage
     */
    fun flush()

    /**
     * Checks whether it is possible to call flush
     */
    fun canFlush(): Boolean
}

enum class StateKind {
    /**
     * Local state for command page. Default type of state parameter
     */
    LOCAL,

    /**
     * Shared state of all command pages of the message
     */
    SHARED,

    /**
     * Shared state of all messages of the user
     */
    USER,


    /**
     * Shared state of all messages between all users
     */
    GLOBAL
}

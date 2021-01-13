package org.github.telegabots.state

import org.github.telegabots.api.StateItem
import org.github.telegabots.api.StateKey

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

    fun flush()
}

interface StateProvider {
    fun get(stateKey: StateKey): StateItem?

    fun set(stateKey: StateKey, value: Any?): StateItem?

    fun flush()

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

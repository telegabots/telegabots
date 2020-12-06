package org.github.telegabots.state

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

data class State(val items: List<StateItem>)

data class StateItem(val key: StateKey,
                     val value: Any) {
    fun equals(type: Class<*>, name: String) = key.equals(type, name)
}

data class StateKey(val type: Class<*>, val name: String) {
    fun equals(type: Class<*>, name: String) = this.type == type && this.name == name
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

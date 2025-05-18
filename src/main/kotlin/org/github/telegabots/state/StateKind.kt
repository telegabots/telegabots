package org.github.telegabots.state

/**
 * Types of states
 */
enum class StateKind {
    /**
     * Local state for page only. Default type of state parameter
     */
    LOCAL,

    /**
     * Shared state of all pages of the message
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

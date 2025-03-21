package org.github.telegabots.api

/**
 * Single state element
 */
data class StateItem(val key: StateKey, val value: Any) {
    fun equals(type: Class<*>, name: String) = key.equals(type, name)
}

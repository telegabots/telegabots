package org.github.telegabots.entity

import org.github.telegabots.api.StateKey

/**
 * Raw state. How it stored in db
 */
data class StateDef(val items: List<StateItemDef>) {
    companion object {
        val Empty = StateDef(emptyList())
    }
}

/**
 * Raw state item. How it stored in db
 */
data class StateItemDef(val key: StateKey,
                        val value: String)

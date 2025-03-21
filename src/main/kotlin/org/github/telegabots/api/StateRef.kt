package org.github.telegabots.api

/**
 * State used while command executing
 */
data class StateRef(val items: List<StateItem>) {
    override fun toString(): String {
        return when {
            items.isEmpty() -> "StateRef()"
            else -> "StateRef(items=\n${items.joinToString("\n")}})"
        }
    }

    companion object {
        /**
         * Empty state
         */
        @JvmField
        val Empty = StateRef(emptyList())

        /**
         * Creates state from list of objects
         */
        @JvmStatic
        fun of(vararg objs: Any): StateRef = StateRef(objs.map { StateItem(key = StateKey.from(it), value = it) })

        /**
         * Creates state from list of name+object
         */
        @JvmStatic
        fun of(vararg objs: Pair<String, Any>): StateRef =
            StateRef(objs.map { StateItem(key = StateKey.from(it.second, it.first), value = it.second) })
    }
}

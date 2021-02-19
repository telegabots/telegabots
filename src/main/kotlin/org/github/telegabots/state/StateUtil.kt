package org.github.telegabots.state

import org.github.telegabots.entity.StateDef

object StateUtil {
    /**
     * Merge state items considering that state1 has higher priority
     */
    fun merge(state1: StateDef?, state2: StateDef?): StateDef? {
        if (state1 == null || state2 == null) {
            return state1 ?: state2
        }

        val result = state1.items.toMutableList()
        val keys = state1.items.map { it.key }.toMutableSet()

        state2.items.forEach {
            if (!keys.contains(it.key)) {
                result.add(it)
                keys.add(it.key)
            }
        }

        return StateDef(result)
    }
}

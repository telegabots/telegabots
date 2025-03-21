package org.github.telegabots.api

/**
 * State key considered as type and name (optional, can be empty)
 */
data class StateKey(val type: Class<*>, val name: String) {
    fun equals(type: Class<*>, name: String) = this.type == type && this.name == name

    override fun toString(): String {
        return if (name.isNotBlank()) "StateKey(${type.name}['$name'])" else "StateKey(${type.name})"
    }

    companion object {
        @JvmStatic
        fun from(obj: Any, name: String = "") = StateKey(type = obj.javaClass, name = name)

        @JvmStatic
        fun from(type: Class<*>, name: String = "") = StateKey(type = type, name = name)
    }
}

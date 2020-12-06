package org.github.telegabots.state


internal class StatesImpl(private val localState: StateProvider,
                          private val sharedState: StateProvider,
                          private val userState: StateProvider,
                          private val globalState: StateProvider) : States {
    override fun get(kind: StateKind, key: StateKey): StateItem? = when (kind) {
        StateKind.LOCAL -> localState.get(key)
        StateKind.SHARED -> sharedState.get(key)
        StateKind.USER -> userState.get(key)
        StateKind.GLOBAL -> globalState.get(key)
    }

    override fun set(kind: StateKind, key: StateKey, value: Any?): StateItem? = when (kind) {
        StateKind.LOCAL -> localState.set(key, value)
        StateKind.SHARED -> sharedState.set(key, value)
        StateKind.USER -> userState.set(key, value)
        StateKind.GLOBAL -> globalState.set(key, value)
    }

    override fun hasValue(kind: StateKind, key: StateKey): Boolean =
            get(kind, key) != null

    override fun flush() {
        listOf(localState, sharedState, userState, globalState)
                .filter { it.canFlush() }
                .forEach { it.flush() }
    }
}

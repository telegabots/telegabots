package org.github.telegabots.state

object EmptyStateProvider : StateProvider {
    override fun get(stateKey: StateKey): StateItem? {
        throw IllegalStateException(ERROR_MESSAGE)
    }

    override fun set(stateKey: StateKey, value: Any?): StateItem? {
        throw IllegalStateException(ERROR_MESSAGE)
    }

    override fun flush() {
        throw IllegalStateException(ERROR_MESSAGE)
    }

    override fun canFlush(): Boolean = false

    private const val ERROR_MESSAGE = "You can not access this kind of state in the command"
}

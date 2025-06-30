package org.github.telegabots.state

import org.github.telegabots.entity.StateDef
import org.github.telegabots.service.InternalJsonService

/**
 * Implementation of [StateProvider] for state type [StateKind.GLOBAL]
 */
internal class GlobalStateProvider(
    private val dbProvider: StateDbProvider,
    private val jsonService: InternalJsonService
) : AbstractStateProvider(jsonService) {
    override fun saveState(state: StateDef) = dbProvider.saveGlobalState(state)

    override fun loadState(): StateDef = dbProvider.getGlobalState()

    override fun toString(): String {
        return "GlobalStateProvider"
    }
}

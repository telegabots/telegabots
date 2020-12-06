package org.github.telegabots.state

import org.github.telegabots.entity.StateDef
import org.github.telegabots.service.JsonService

class GlobalStateProvider(private val dbProvider: StateDbProvider,
                          private val jsonService: JsonService) : AbstractStateProvider(jsonService) {
    override fun saveState(state: StateDef) = dbProvider.saveGlobalState(state)

    override fun loadState(): StateDef = dbProvider.getGlobalState()
}

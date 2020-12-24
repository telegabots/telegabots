package org.github.telegabots.state

import org.github.telegabots.entity.StateDef
import org.github.telegabots.service.JsonService

class LocalStateProvider(
    private val pageId: Long,
    private val state: StateDef?,
    private val dbProvider: StateDbProvider,
    private val jsonService: JsonService
) : AbstractStateProvider(jsonService) {
    override fun saveState(state: StateDef) = dbProvider.saveLocalState(pageId, state)

    override fun loadState(): StateDef = state ?: dbProvider.getLocalState(pageId)
}

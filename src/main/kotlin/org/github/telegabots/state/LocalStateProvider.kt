package org.github.telegabots.state

import org.github.telegabots.entity.StateDef
import org.github.telegabots.service.InternalJsonService

/**
 * Implementation of [StateProvider] for state type [StateKind.LOCAL]
 */
internal class LocalStateProvider(
    private val pageId: Long,
    private val dbProvider: StateDbProvider,
    private val jsonService: InternalJsonService
) : AbstractStateProvider(jsonService) {
    override fun saveState(state: StateDef) = dbProvider.saveLocalState(pageId, state)

    override fun loadState(): StateDef = dbProvider.getLocalState(pageId)

    override fun toString(): String {
        return "LocalStateProvider(pageId=$pageId)"
    }
}

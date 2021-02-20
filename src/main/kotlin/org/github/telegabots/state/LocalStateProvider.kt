package org.github.telegabots.state

import org.github.telegabots.api.StateItem
import org.github.telegabots.api.StateKey
import org.github.telegabots.entity.StateDef
import org.github.telegabots.service.JsonService

/**
 * Local state provider related with page and provides priority access to additional state (optional)
 *
 * Note: additional state will not store to db
 */
class LocalStateProvider(
    private val pageId: Long,
    private val state: StateDef?,
    private val dbProvider: StateDbProvider,
    private val jsonService: JsonService
) : AbstractStateProvider(jsonService) {
    private val localCache: Map<StateKey, StateItem> = fromStateDef(state, jsonService)

    override fun saveState(state: StateDef) = dbProvider.saveLocalState(pageId, state)

    override fun loadState(): StateDef = dbProvider.getLocalState(pageId)

    override fun toString(): String {
        return "LocalStateProvider(pageId=$pageId, local state is null=${state == null})"
    }

    override fun get(key: StateKey): StateItem? {
        return localCache[key] ?: super.get(key)
    }

    override fun getAll(): List<StateItem> {
        val list = localCache.values.toMutableList()

        synchronized(cache) {
            list.addAll(cache.values.filter { !localCache.containsKey(it.key) })
        }

        return list
    }

    companion object {
        private fun fromStateDef(state: StateDef?, jsonService: JsonService): Map<StateKey, StateItem> =
            if (state != null) {
                state.items.map { it.key to jsonService.toStateItem(it) }.toMap()
            } else emptyMap()
    }
}

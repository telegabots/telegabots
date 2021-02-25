package org.github.telegabots.state

import org.github.telegabots.api.StateItem
import org.github.telegabots.api.StateKey
import org.github.telegabots.entity.StateDef
import org.github.telegabots.service.JsonService

/**
 * Composition of specified state provider with priority access to additional state
 */
class AdditionalStateProvider(
    private val provider: StateProvider,
    private val additional: StateDef,
    private val jsonService: JsonService
) : StateProvider {
    private val localCache: MutableMap<StateKey, StateItem> = fromStateDef(additional, jsonService)

    override fun get(key: StateKey): StateItem? {
        return synchronized(localCache) {
            localCache[key]
        } ?: provider.get(key)
    }

    override fun set(key: StateKey, value: Any?): StateItem? {
        synchronized(localCache) {
            localCache.remove(key)
        }

        return provider.set(key, value)
    }

    override fun canFlush(): Boolean = provider.canFlush()

    override fun flush() = provider.flush()

    override fun setAll(items: List<StateItem>) {
        synchronized(localCache) {
            items.forEach { localCache.remove(it.key) }
        }

        provider.setAll(items)
    }

    override fun getAll(): List<StateItem> {
        val providerItems = provider.getAll()

        synchronized(localCache) {
            val list = localCache.values.toMutableList()
            list.addAll(providerItems.filter { !localCache.containsKey(it.key) })

            return list
        }
    }

    companion object {
        private fun fromStateDef(state: StateDef, jsonService: JsonService): MutableMap<StateKey, StateItem> =
            state.items.map { it.key to jsonService.toStateItem(it) }.toMap().toMutableMap()
    }
}

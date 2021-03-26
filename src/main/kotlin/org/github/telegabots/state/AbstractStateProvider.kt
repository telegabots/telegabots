package org.github.telegabots.state

import org.github.telegabots.api.StateItem
import org.github.telegabots.api.StateKey
import org.github.telegabots.entity.StateDef
import org.github.telegabots.service.JsonService
import org.slf4j.LoggerFactory

abstract class AbstractStateProvider(private val jsonService: JsonService) : StateProvider {
    protected val log = LoggerFactory.getLogger(javaClass)!!
    private val cache: MutableMap<StateKey, StateItem> = mutableMapOf()
    private var initted: Boolean = false
    // TODO: add dirty flag

    override fun get(key: StateKey): StateItem? {
        return synchronized(cache) {
            loadFromDb()
            cache[key]
        }
    }

    override fun set(key: StateKey, value: Any?): StateItem? {
        if (log.isTraceEnabled) {
            log.trace("Set state of type: {}, value: {}, by {}", value?.javaClass?.name, value, this)
        }

        return synchronized(cache) {
            loadFromDb()

            if (value == null) {
                cache.remove(key)
            } else {
                cache.put(key, StateItem(key, value))
            }
        }
    }

    override fun getAll(): List<StateItem> {
        return synchronized(cache) {
            loadFromDb()

            cache.values.toList()
        }
    }

    override fun mergeAll(items: List<StateItem>) {
        if (log.isTraceEnabled) {
            log.trace("set state items: {}", items)
        }

        synchronized(cache) {
            items.forEach { item -> cache[item.key] = item }
        }
    }

    /**
     * Save state to permanent store
     */
    abstract fun saveState(state: StateDef)

    /**
     * Load state from permanent store
     */
    abstract fun loadState(): StateDef

    /**
     * Can be stored into permanent store
     */
    override fun canFlush(): Boolean = true

    override fun flush() {
        val state = synchronized(cache) {
            StateDef(cache.values.map { jsonService.toStateItemDef(it) })
        }

        saveState(state)
    }

    private fun loadFromDb() {
        if (!initted) {
            initted = true
            loadState().items
                .map { jsonService.toStateItem(it) }
                .forEach { cache[it.key] = it }
        }
    }
}

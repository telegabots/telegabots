package org.github.telegabots.state

import org.github.telegabots.api.StateItem
import org.github.telegabots.api.StateKey
import org.github.telegabots.entity.StateDef
import org.github.telegabots.service.JsonService
import org.slf4j.LoggerFactory

abstract class AbstractStateProvider(private val jsonService: JsonService) : StateProvider {
    protected val log = LoggerFactory.getLogger(javaClass)!!
    protected val cache: MutableMap<StateKey, StateItem> = mutableMapOf()
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

    abstract fun saveState(state: StateDef)

    abstract fun loadState(): StateDef

    override fun canFlush(): Boolean = true

    override fun flush() {
        synchronized(cache) {
            saveState(StateDef(cache.values.map { jsonService.toStateItemDef(it) }))
        }
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

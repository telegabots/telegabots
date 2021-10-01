package org.github.telegabots.service

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.github.telegabots.api.Service
import org.github.telegabots.api.StateItem
import org.github.telegabots.api.StateRef
import org.github.telegabots.entity.StateDef
import org.github.telegabots.entity.StateItemDef

open class JsonService : Service {
    private val objectMapper = ObjectMapper()

    init {
        objectMapper.enable(MapperFeature.USE_ANNOTATIONS)
        objectMapper.registerModule(JavaTimeModule())
        objectMapper.registerKotlinModule()
    }

    fun <T> parse(str: String, clazz: Class<T>): T = objectMapper.readValue(str, clazz)

    fun toJson(obj: Any): String = objectMapper.writeValueAsString(obj)

    fun toPrettyJson(obj: Any): String = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj)

    fun toStateItem(item: StateItemDef): StateItem =
        StateItem(key = item.key, value = parse(item.value, item.key.type))

    fun toStateItemDef(item: StateItem): StateItemDef =
        StateItemDef(key = item.key, value = toJson(item.value))

    fun toStateDef(state: StateRef?): StateDef? =
        state?.let { StateDef(items = state.items.map { toStateItemDef(it) }) }

    fun toStateDefFrom(vararg objs: Any): StateDef = toStateDef(StateRef.of(*objs))!!

    fun toState(stateDef: StateDef?): StateRef? =
        stateDef?.let { StateRef(items = stateDef.items.map { toStateItem(it) }) }
}

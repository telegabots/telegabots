package org.github.telegabots.service

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.github.telegabots.api.JsonService
import org.github.telegabots.api.StateItem
import org.github.telegabots.api.StateRef
import org.github.telegabots.entity.StateDef
import org.github.telegabots.entity.StateItemDef

internal open class InternalJsonService : JsonService {
    protected val objectMapper = ObjectMapper()

    init {
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        objectMapper.registerModule(JavaTimeModule())
        objectMapper.registerKotlinModule()
    }

    override fun <T> parse(str: String, clazz: Class<T>): T = objectMapper.readValue(str, clazz)

    override fun toJson(obj: Any): String = objectMapper.writeValueAsString(obj)

    override fun toPrettyJson(obj: Any): String = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj)

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

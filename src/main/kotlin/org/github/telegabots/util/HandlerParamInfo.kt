package org.github.telegabots.util

import org.github.telegabots.api.CommandContext
import org.github.telegabots.api.Service
import org.github.telegabots.api.State
import org.github.telegabots.api.StateKey
import org.github.telegabots.api.UserService
import org.github.telegabots.state.StateKind
import org.github.telegabots.state.States
import kotlin.reflect.full.isSuperclassOf

data class HandlerParamInfo(
    val stateKind: StateKind,
    val stateName: String,
    val type: Class<*>,
    val innerType: Class<*>?
) {
    fun isState(): Boolean = State::class.isSuperclassOf(type.kotlin)

    fun isContext(): Boolean = CommandContext::class.isSuperclassOf(type.kotlin)

    fun isService(): Boolean = Service::class.isSuperclassOf(type.kotlin)

    fun isUserService(): Boolean = UserService::class.isSuperclassOf(type.kotlin)

    fun isString(): Boolean = type == String::class.java

    fun isInteger(): Boolean = type == Int::class.java
}

class StateImpl(private val param: HandlerParamInfo, private val states: States) : State<Any?> {
    private val key = StateKey(param.innerType!!, param.stateName)

    override fun get(): Any? {
        return states.get(param.stateKind, key)?.value
    }

    override fun set(value: Any?): Any? {
        return states.set(param.stateKind, key, value)?.value
    }

    override fun isPresent(): Boolean = states.hasValue(param.stateKind, key)
}

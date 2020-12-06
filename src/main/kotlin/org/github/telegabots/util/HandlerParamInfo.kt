package org.github.telegabots.util

import org.github.telegabots.CommandContext
import org.github.telegabots.Service
import org.github.telegabots.State
import org.github.telegabots.state.StateKind
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf

data class HandlerParamInfo(
    val stateKind: StateKind,
    val stateName: String,
    val type: Class<*>,
    val innerType: Class<*>?
) {
    fun isState(): Boolean = State::class.isSuperclassOf(type.kotlin)

    fun isContext(): Boolean = CommandContext::class.isSubclassOf(type.kotlin)

    fun isService(): Boolean = Service::class.isSubclassOf(type.kotlin)

    fun isString(): Boolean = type == String::class.java

    fun isInteger(): Boolean = type == Int::class.java
}

package org.github.telegabots.util

import org.github.telegabots.api.CommandContext
import org.github.telegabots.api.Service
import org.github.telegabots.api.State
import org.github.telegabots.api.UserService
import org.github.telegabots.state.StateKind
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

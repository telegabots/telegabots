package org.github.telegabots.util

import org.github.telegabots.api.ControllerContext
import org.github.telegabots.api.Service
import org.github.telegabots.api.State
import org.github.telegabots.api.UserService
import org.github.telegabots.state.StateKind
import kotlin.reflect.full.isSuperclassOf

/**
 * Information about type and name of a parameter in a handler method
 */
internal data class HandlerParamInfo(
    val stateKind: StateKind,
    val stateName: String,
    val type: Class<*>,
    val innerType: Class<*>?
) {
    fun isState(): Boolean = State::class.isSuperclassOf(type.kotlin)

    fun isContext(): Boolean = ControllerContext::class.isSuperclassOf(type.kotlin)

    fun isService(): Boolean = Service::class.isSuperclassOf(type.kotlin)

    fun isUserService(): Boolean = UserService::class.isSuperclassOf(type.kotlin)

    fun isString(): Boolean = type == String::class.java

    fun isInteger(): Boolean = type == Int::class.java
}

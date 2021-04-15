package org.github.telegabots.util

import org.github.telegabots.api.State
import org.github.telegabots.api.annotation.Global
import org.github.telegabots.api.annotation.Local
import org.github.telegabots.api.annotation.Shared
import org.github.telegabots.api.annotation.User
import org.github.telegabots.state.StateKind
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.lang.reflect.ParameterizedType
import java.util.stream.Stream
import kotlin.reflect.full.isSuperclassOf

object HandlerParamUtil {
    fun getParams(method: Method): List<HandlerParamInfo> =
        method.parameters.map { mapParam(it) }

    private fun mapParam(param: Parameter): HandlerParamInfo {
        val global = param.annotations
            .filter { it.annotationClass == Global::class }
            .map { it as Global }
            .firstOrNull()
        val shared = param.annotations
            .filter { it.annotationClass == Shared::class }
            .map { it as Shared }
            .firstOrNull()
        val local = param.annotations
            .filter { it.annotationClass == Local::class }
            .map { it as Local }
            .firstOrNull()
        val user = param.annotations
            .filter { it.annotationClass == User::class }
            .map { it as User }
            .firstOrNull()

        val (stateKind, stateName) = getKind(local, shared, global, user)

        if (State::class.isSuperclassOf(param.type.kotlin)) {
            val parType = param.parameterizedType as ParameterizedType
            return HandlerParamInfo(
                type = param.type,
                innerType = parType.actualTypeArguments[0] as Class<*>,
                stateName = stateName,
                stateKind = stateKind
            )
        }

        return HandlerParamInfo(
            type = param.type, innerType = null,
            stateName = stateName,
            stateKind = stateKind
        )
    }

    private fun getKind(local: Local?, shared: Shared?, global: Global?, user: User?): Pair<StateKind, String> {
        if (Stream.of(local, shared, global, user).filter { it != null }.count() > 1) {
            throw IllegalStateException(SIM_STATES_ERROR)
        }

        return when {
            local != null -> StateKind.LOCAL to local.name.trim()
            shared != null -> StateKind.SHARED to shared.name.trim()
            user != null -> StateKind.USER to user.name.trim()
            global != null -> StateKind.GLOBAL to global.name.trim()
            else -> StateKind.LOCAL to ""
        }
    }

    private const val SIM_STATES_ERROR = "State cannot be local, shared or global simultaneously"
}

package org.github.telegabots.util

import org.github.telegabots.api.BaseTask
import org.github.telegabots.api.Service
import org.github.telegabots.api.StateKey
import org.github.telegabots.api.TaskContext
import org.github.telegabots.api.UserService
import org.github.telegabots.error.TaskInvokeException
import org.github.telegabots.state.States
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

data class TaskHandlerInfo(val name: String,
                           val params: List<HandlerParamInfo>,
                           val method: Method,
                           val retType: Class<*>,
                           val task: BaseTask) {
    fun executeInline(query: String, states: States, context: TaskContext) {
        try {
            val args = toArgs(query, states, context)

            method.invoke(task, *args)
        } catch (ex: Throwable) {
            throw TaskInvokeException(task.javaClass, getInnerException(ex))
        }
    }

    private fun getInnerException(ex: Throwable): Throwable = when (ex) {
        is InvocationTargetException -> ex.targetException
        else -> ex
    }

    private fun toArgs(text: String, states: States, context: TaskContext): Array<Any?> {
        return Array(params.size) { idx ->
            if (idx == 0) text else toArg(params[idx], states, context)
        }
    }

    private fun toArg(param: HandlerParamInfo, states: States, context: TaskContext): Any? {
        if (param.isState()) {
            return StateImpl(param, states)
        }

        if (param.isUserService()) {
            val service = context.getUserService(param.type as Class<UserService>)
            check(service != null) { "Service not found: ${param.type.name}" }
            return service
        }

        if (param.isService()) {
            val service = context.getService(param.type as Class<Service>)
            check(service != null) { "Service not found: ${param.type.name}" }
            return service
        }

        return states.get(param.stateKind, StateKey(param.type, param.stateName))?.value
    }

    fun isValidReturnType() = retType == Boolean::class.java || isVoidReturnType()

    fun isVoidReturnType() = retType.name == "void" || retType == Void::class.java
}

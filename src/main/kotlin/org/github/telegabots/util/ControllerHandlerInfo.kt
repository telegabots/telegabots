package org.github.telegabots.util

import org.github.telegabots.api.*
import org.github.telegabots.error.ControllerInvokeException
import org.github.telegabots.state.States
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Information about handler method in [BaseController]
 */
internal data class ControllerHandlerInfo(
    val name: String,
    val handlerType: HandlerType,
    val params: List<HandlerParamInfo>,
    val method: Method,
    val retType: Class<*>,
    val controller: BaseController
) {
    fun executeText(text: String, states: States, context: ControllerContext): Boolean {
        check(handlerType == HandlerType.Text) { "Invalid handler type: $handlerType. Expected Text type" }

        try {
            val args = toArgs(text, states, context)

            return (method.invoke(controller, *args) ?: true) as Boolean
        } catch (ex: Throwable) {
            throw ControllerInvokeException(controller.javaClass, getInnerException(ex))
        }
    }

    fun executeInline(query: String, states: States, context: ControllerContext) {
        check(handlerType == HandlerType.Inline) { "Invalid handler type: $handlerType. Expected Inline type" }


        try {
            val args = toArgs(query, states, context)

            method.invoke(controller, *args)
        } catch (ex: Throwable) {
            throw ControllerInvokeException(controller.javaClass, getInnerException(ex))
        }
    }

    fun handleError(exception: ControllerInvokeException, states: States, context: ControllerContext, message: String) {
        check(handlerType == HandlerType.Error) { "Invalid handler type: $handlerType. Expected Error type" }

        try {
            val args = toArgs(exception.cause!!, message, states, context)

            method.invoke(controller, *args)
        } catch (ex: Throwable) {
            throw ControllerInvokeException(controller.javaClass, getInnerException(ex))
        }
    }

    private fun getInnerException(ex: Throwable): Throwable = when (ex) {
        is InvocationTargetException -> ex.targetException
        else -> ex
    }

    private fun toArgs(text: String, states: States, context: ControllerContext): Array<Any?> {
        return Array(params.size) { idx ->
            if (idx == 0) text else toArg(params[idx], states, context)
        }
    }

    private fun toArgs(exception: Throwable, text: String, states: States, context: ControllerContext): Array<Any?> {
        return Array(params.size) { idx ->
            if (idx == 0)
                exception
            else if (idx == 1)
                text // the second parameter is always message
            else toArg(params[idx], states, context)
        }
    }

    private fun toArg(param: HandlerParamInfo, states: States, context: ControllerContext): Any? {
        if (param.isState()) {
            return StateImpl(param, states)
        }

        if (param.isUserService()) {
            return context.getUserService(param.type as Class<UserService>)
        }

        if (param.isService()) {
            return context.getService(param.type as Class<Service>)
        }

        return states.get(param.stateKind, StateKey(param.type, param.stateName))?.value
    }

    fun isValidReturnType() = retType == Boolean::class.java || isVoidReturnType()

    fun isVoidReturnType() = retType.name == "void" || retType == Void::class.java
}

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
    val messageType: MessageType,
    val params: List<HandlerParamInfo>,
    val method: Method,
    val retType: Class<*>,
    val controller: BaseController
) {
    fun executeText(text: String, states: States, context: ControllerContext): Boolean {
        check(messageType == MessageType.Text) { "Invalid message type: $messageType" }

        try {
            val args = toArgs(text, states, context)

            return (method.invoke(controller, *args) ?: true) as Boolean
        } catch (ex: Throwable) {
            throw ControllerInvokeException(controller.javaClass, getInnerException(ex))
        }
    }

    fun executeInline(query: String, states: States, context: ControllerContext) {
        check(messageType == MessageType.Inline) { "Invalid message type: $messageType" }

        try {
            val args = toArgs(query, states, context)

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

package org.github.telegabots.util

import org.github.telegabots.*
import org.github.telegabots.error.CommandInvokeException
import org.github.telegabots.state.StateKey
import org.github.telegabots.state.States
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

data class HandlerInfo(
    val name: String,
    val messageType: MessageType,
    val params: List<HandlerParamInfo>,
    val method: Method,
    val retType: Class<*>,
    val command: BaseCommand
) {
    fun execute(text: String, states: States, context: CommandContext): Boolean {
        check(messageType == MessageType.TEXT) { "Invalid message type: $messageType" }

        try {
            if (params.size == 1) {
                return (method.invoke(command, text) ?: true) as Boolean
            }

            val args = toArgs(text, states, context)

            return (method.invoke(command, *args) ?: true) as Boolean
        } catch (ex: Throwable) {
            throw CommandInvokeException(command.javaClass, getInnerException(ex))
        }
    }

    fun executeCallback(messageId: Int, query: String, states: States, context: CommandContext) {
        check(messageType == MessageType.CALLBACK) { "Invalid message type: $messageType" }

        try {
            val args = toArgs(messageId, query, states, context)

            method.invoke(command, *args)
        } catch (ex: Throwable) {
            throw CommandInvokeException(command.javaClass, getInnerException(ex))
        }
    }

    private fun getInnerException(ex: Throwable): Throwable = when (ex) {
        is InvocationTargetException -> ex.targetException
        else -> ex
    }

    private fun toArgs(messageId: Int, query: String, states: States, context: CommandContext): Array<Any?> {
        return Array(params.size) { idx ->
            when (idx) {
                0, 1 -> if (params[idx].isInteger()) messageId else query
                else -> toArg(params[idx], states, context)
            }
        }
    }

    private fun toArgs(text: String, states: States, context: CommandContext): Array<Any?> {
        return Array(params.size) { idx ->
            if (idx == 0) text else toArg(params[idx], states, context)
        }
    }

    private fun toArg(param: HandlerParamInfo, states: States, context: CommandContext): Any? {
        if (param.isState()) {
            return StateImpl(param, states)
        }

        if (param.isContext()) {
            return context
        }

        if (param.isService()) {
            val service = context.getService(param.type as Class<Service>)
            check(service != null) { "Service not found: ${param.type.name}" }
            return service
        }

        return states.get(param.stateKind, StateKey(param.type, param.stateName))?.value
    }

    fun isValidReturnType() = retType == Boolean::class.java || retType.name == "void" || retType == Void::class.java

    fun isVoidReturnType() = retType.name == "void" || retType == Void::class.java
}


private class StateImpl(private val param: HandlerParamInfo, private val states: States) : State<Any?> {
    private val key = StateKey(param.innerType!!, param.stateName)

    override fun get(): Any? {
        return states.get(param.stateKind, key)?.value
    }

    override fun set(value: Any?): Any? {
        return states.set(param.stateKind, key, value)?.value
    }

    override fun isPresent(): Boolean = states.hasValue(param.stateKind, key)
}

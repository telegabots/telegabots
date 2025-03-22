package org.github.telegabots.util

import org.github.telegabots.api.State
import org.github.telegabots.api.StateKey
import org.github.telegabots.state.States

/**
 * Implementation of [State] for [HandlerParamInfo]
 */
internal class StateImpl(private val param: HandlerParamInfo, private val states: States) : State<Any?> {
    private val key = StateKey(param.innerType!!, param.stateName)

    override fun get(): Any? {
        return states.get(param.stateKind, key)?.value
    }

    override fun set(value: Any?): Any? {
        return states.set(param.stateKind, key, value)?.value
    }

    override fun isPresent(): Boolean = states.hasValue(param.stateKind, key)
}

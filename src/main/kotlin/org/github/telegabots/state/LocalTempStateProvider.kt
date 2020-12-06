package org.github.telegabots.state

import org.github.telegabots.entity.StateDef
import org.github.telegabots.service.JsonService

/**
 * Used as Local state for CommandDef. Can be flushed
 */
internal class LocalTempStateProvider(private val state: StateDef?,
                                      private val jsonService: JsonService) : AbstractStateProvider(jsonService) {
    override fun saveState(state: StateDef) {
        throw IllegalStateException("Temp local state cannot be flushed")
    }

    override fun loadState(): StateDef = state ?: StateDef.Empty

    override fun canFlush(): Boolean = false
}

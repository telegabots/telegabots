package org.github.telegabots.state

import org.github.telegabots.entity.StateDef
import org.github.telegabots.service.JsonService

class SharedStateProvider(
    private val userId: Long,
    private val messageId: Int,
    private val dbProvider: StateDbProvider,
    private val jsonService: JsonService
) : AbstractStateProvider(jsonService) {
    override fun saveState(state: StateDef) = dbProvider.saveSharedState(userId, messageId, state)

    override fun loadState(): StateDef = dbProvider.getSharedState(userId, messageId)

    override fun toString(): String {
        return "SharedStateProvider(userId=$userId, messageId=$messageId)"
    }
}

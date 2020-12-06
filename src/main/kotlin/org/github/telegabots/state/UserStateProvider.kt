package org.github.telegabots.state

import org.github.telegabots.entity.StateDef
import org.github.telegabots.service.JsonService

class UserStateProvider(private val userId: Int,
                        private val dbProvider: StateDbProvider,
                        private val jsonService: JsonService) : AbstractStateProvider(jsonService) {
    override fun saveState(state: StateDef) = dbProvider.saveUserState(userId, state)

    override fun loadState(): StateDef = dbProvider.getUserState(userId)
}

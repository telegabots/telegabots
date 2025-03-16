package org.github.telegabots.state

import org.github.telegabots.api.UserService
import org.github.telegabots.entity.StateDef
import org.github.telegabots.service.JsonService

internal class UserStateProvider(
    private val userId: Long,
    private val dbProvider: StateDbProvider,
    private val jsonService: JsonService
) : AbstractStateProvider(jsonService), UserService {

    override fun userId(): Long = userId

    override fun saveState(state: StateDef) = dbProvider.saveUserState(userId, state)

    override fun loadState(): StateDef = dbProvider.getUserState(userId)

    override fun toString(): String {
        return "UserStateProvider(userId=$userId)"
    }
}

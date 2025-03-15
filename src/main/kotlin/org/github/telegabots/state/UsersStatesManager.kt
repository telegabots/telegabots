package org.github.telegabots.state

import org.github.telegabots.api.LocalizationFactory
import org.github.telegabots.service.JsonService


/**
 * States manager used by all users
 */
class UsersStatesManager(
    private val dbProvider: LockableStateDbProvider,
    private val localizationFactory: LocalizationFactory,
    private val jsonService: JsonService
) {
    private val userStatesServices: MutableMap<Long, UserStateService> = mutableMapOf()
    private val globalState: GlobalStateProvider = GlobalStateProvider(dbProvider, jsonService)

    /**
     * Returns state service for specified user
     */
    fun get(userId: Long): UserStateService {
        return synchronized(userStatesServices) {
            userStatesServices.getOrPut(userId) {
                UserStateService(
                    userId,
                    dbProvider,
                    localizationFactory,
                    jsonService,
                    globalState
                )
            }
        }
    }
}

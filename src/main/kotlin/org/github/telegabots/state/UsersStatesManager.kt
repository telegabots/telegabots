package org.github.telegabots.state

import org.github.telegabots.service.JsonService
import org.github.telegabots.service.UserLocalizationFactory


/**
 * States manager used by all users
 */
class UsersStatesManager(
    private val dbProvider: StateDbProvider,
    private val localizationFactory: UserLocalizationFactory,
    private val jsonService: JsonService
) {
    private val userStatesServices: MutableMap<Int, UserStateService> = mutableMapOf()
    private val globalState: GlobalStateProvider = GlobalStateProvider(dbProvider, jsonService)

    /**
     * Returns state service for specified user
     */
    fun get(userId: Int): UserStateService {
        return synchronized(userStatesServices) {
            val localizeProvider = localizationFactory.getProvider(userId)

            userStatesServices.getOrPut(userId) {
                UserStateService(
                    userId,
                    dbProvider,
                    localizeProvider,
                    jsonService,
                    globalState
                )
            }
        }
    }
}

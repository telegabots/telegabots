package org.github.telegabots.state

import org.github.telegabots.api.UserLocalizationFactory
import org.github.telegabots.service.JsonService


/**
 * States manager used by all users
 */
class UsersStatesManager(
    private val dbProvider: LockableStateDbProvider,
    private val localizationFactory: UserLocalizationFactory,
    private val jsonService: JsonService
) {
    private val userStatesServices: MutableMap<Long, UserStateService> = mutableMapOf()
    private val globalState: GlobalStateProvider = GlobalStateProvider(dbProvider, jsonService)

    /**
     * Returns state service for specified user
     */
    fun get(userId: Long): UserStateService {
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

package org.github.telegabots.service

import org.github.telegabots.api.*
import org.github.telegabots.state.*

/**
 * Internal implementation of [ServiceProvider]
 */
internal class InternalServiceProvider(
    private val delegate: ServiceProvider,
    private val stateDbProvider: LockableStateDbProvider,
    private val jsonService: JsonService
) : ServiceProvider {
    private val globalState: GlobalStateProvider = GlobalStateProvider(stateDbProvider, jsonService)

    // TODO: clean cache when needed
    private val serviceCache: MutableMap<Class<*>, Service?> = HashMap()
    private val userServiceCache: MutableMap<Pair<Class<*>, Long>, UserService?> = HashMap()

    init {
        delegate.setInternalService(this)
    }

    override fun <T : Service> getService(clazz: Class<T>): T? {
        // TODO: detect circular dependencies
        synchronized(serviceCache) {
            // we cannot use computeIfAbsent because getServiceInternal can call getService
            var service = serviceCache[clazz]
            if (service == null) {
                service = getServiceInternal(clazz)
                if (service != null) {
                    serviceCache[clazz] = service
                }
            }
            return service as T?
        }
    }

    override fun <T : UserService> getUserService(clazz: Class<T>, userId: Long): T?  {
        // TODO: detect circular dependencies
        synchronized(userServiceCache) {
            // we cannot use computeIfAbsent because getUserServiceInternal can call getService
            val key = Pair(clazz, userId)
            var service = userServiceCache[key]
            if (service == null) {
                service = getUserServiceInternal(clazz, userId)
                if (service != null) {
                    userServiceCache[key] = service
                }
            }
            return service as T?
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Service> getServiceInternal(clazz: Class<T>): T? {
        var service = when (clazz) {
            JsonService::class.java -> jsonService
            StateDbProvider::class.java -> stateDbProvider
            else -> null
        }
        if (service == null) {
            // get user defined service
            service = delegate.getService(clazz)

            if (service == null) {
                service = when (clazz) {
                    LocalizationFactory::class.java -> FileBasedLocalizationFactory(jsonService)
                    else -> null
                }
            }
        }

        return service as T?
    }

    private fun <T : UserService> getUserServiceInternal(clazz: Class<T>, userId: Long): T? {
        var service = when (clazz) {
            UserStateProvider::class.java -> UserStateProvider(
                userId,
                stateDbProvider,
                jsonService
            )

            UserStateService::class.java -> UserStateService(
                userId,
                stateDbProvider,
                jsonService,
                globalState,
                this
            )

            UserSettingsService::class.java -> UserSettingsService(
                getUserService(
                    UserStateProvider::class.java,
                    userId
                )!!
            )

            UserLocalizationProvider::class.java -> UserLocalizationProviderImpl(
                getService(LocalizationFactory::class.java)!!,
                getUserService(UserSettingsService::class.java, userId)!!
            )

            else -> null
        } as T?

        if (service == null) {
            // get user defined service
            service = delegate.getUserService(clazz, userId)

            if (service == null) {
                // TODO: add post init user services
            }
        }

        return service as T?
    }
}

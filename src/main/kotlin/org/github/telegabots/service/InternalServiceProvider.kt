package org.github.telegabots.service

import org.github.telegabots.api.*
import org.github.telegabots.api.config.BotConfig
import org.github.telegabots.sqlite.SqliteEntityRepositoryFactory
import org.github.telegabots.state.*
import org.github.telegabots.state.sqlite.SqliteStateDbProvider
import org.github.telegabots.util.SqliteConnectionUtil
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.sql.Connection
import java.util.function.Supplier

/**
 * Internal implementation of [ServiceProvider]
 */
internal class InternalServiceProvider(
    private val userServiceProvider: ServiceProvider,
    private val jsonService: JsonService,
    private val config: BotConfig
) : ServiceProvider {

    // TODO: clean cache when needed, use LRU cache
    private val serviceCache: MutableMap<Class<*>, Service?> = HashMap()
    private val userServiceCache: MutableMap<Pair<Class<*>, Long>, UserService?> = HashMap()
    private val supplierCache: MutableMap<Class<*>, Supplier<*>?> = HashMap()

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

    override fun <T : UserService> getUserService(clazz: Class<T>, userId: Long): T? {
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

    override fun <T> getSupplier(clazz: Class<T>): Supplier<T>? {
        synchronized(supplierCache) {
            // we cannot use computeIfAbsent because getSupplierInternal can call getService
            var supplier = supplierCache[clazz]
            if (supplier == null) {
                supplier = getSupplierInternal(clazz)
                if (supplier != null) {
                    supplierCache[clazz] = supplier
                }
            }
            return supplier as Supplier<T>?
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Service> getServiceInternal(clazz: Class<T>): T? {
        var service = when (clazz) {
            JsonService::class.java -> jsonService
            LockableStateDbProvider::class.java -> LockableStateDbProvider.of(getService(StateDbProvider::class.java)!!)
            GlobalStateProvider::class.java -> GlobalStateProvider(
                getService(LockableStateDbProvider::class.java)!!,
                jsonService
            )

            SqliteStateDbProvider::class.java -> SqliteStateDbProvider(
                getSupplier(DSLContext::class.java)!!.get(),
                jsonService
            )

            else -> null
        }
        if (service == null) {
            // get user defined service
            service = userServiceProvider.getService(clazz)

            if (service == null) {
                service = when (clazz) {
                    LocalizationFactory::class.java -> FileBasedLocalizationFactory(jsonService)
                    StateDbProvider::class.java -> getService(SqliteStateDbProvider::class.java)
                    else -> null
                }
            }
            // special case for StateDbProvider
            if (service is StateDbProvider) {
                service = LockableStateDbProvider.of(service)
            }
        }

        return service as T?
    }

    private fun <T : UserService> getUserServiceInternal(clazz: Class<T>, userId: Long): T? {
        var service = when (clazz) {
            UserStateProvider::class.java -> UserStateProvider(
                userId,
                getService(StateDbProvider::class.java)!!,
                jsonService
            )

            UserStateService::class.java -> UserStateService(
                userId,
                getService(LockableStateDbProvider::class.java)!!,
                jsonService,
                getService(GlobalStateProvider::class.java)!!,
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

            EntityRepositoryFactory::class.java -> SqliteEntityRepositoryFactory(
                userId,
                getSupplier(DSLContext::class.java)!!.get(),
                jsonService
            )

            else -> null
        } as T?

        if (service == null) {
            // get user defined service
            service = userServiceProvider.getUserService(clazz, userId)

            if (service == null) {
                // TODO: add post init user services
            }
        }

        return service as T?
    }

    private fun <T> getSupplierInternal(clazz: Class<T>): Supplier<T>? {
        var supplier: Supplier<T>? = when (clazz) {
            Connection::class.java -> Supplier { SqliteConnectionUtil.getConnection(config.stateDbPath) as T }
            DSLContext::class.java -> Supplier { DSL.using(getSupplier(Connection::class.java)!!.get()) as T }
            else -> null
        }

        if (supplier == null) {
            // get user defined supplier
            supplier = userServiceProvider.getSupplier(clazz)

            if (supplier == null) {
                // TODO: add post init suppliers
            }
        }

        return supplier
    }
}

package org.github.telegabots.service

import org.github.telegabots.api.InputUser
import org.github.telegabots.api.Service
import org.github.telegabots.api.ServiceProvider
import org.github.telegabots.api.UserLocalizationFactory
import org.github.telegabots.api.UserService

class InternalServiceProvider(
    private val delegate: ServiceProvider,
    private val jsonService: JsonService
) : ServiceProvider {
    init {
        delegate.setInternalService(this)
    }

    override fun <T : Service> getService(clazz: Class<T>): T? =
        (getServiceInternalPre(clazz) ?: delegate.getService(clazz)) ?: getServiceInternalPost(clazz)

    override fun <T : UserService> getUserService(clazz: Class<T>, user: InputUser): T? =
        delegate.getUserService(clazz, user)

    @Suppress("UNCHECKED_CAST")
    private fun <T : Service> getServiceInternalPre(clazz: Class<T>): T? {
        val service = when (clazz) {
            JsonService::class.java -> jsonService
            else -> null
        }

        return service as T?
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Service> getServiceInternalPost(clazz: Class<T>): T? {
        val service = when (clazz) {
            UserLocalizationFactory::class.java -> FileBasedLocalizationFactory(jsonService)
            else -> null
        }

        return service as T?
    }
}

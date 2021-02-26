package org.examples.allfeaturedbot

import org.github.telegabots.api.Service
import org.github.telegabots.api.ServiceProvider
import org.github.telegabots.api.UserService

class ServiceProviderImpl : ServiceProvider {
    override fun <T : Service> getService(clazz: Class<T>): T? = null

    override fun <T : UserService> getUserService(clazz: Class<T>, userId: Int): T? = null
}

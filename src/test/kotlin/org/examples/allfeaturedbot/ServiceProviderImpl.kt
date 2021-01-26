package org.examples.allfeaturedbot

import org.github.telegabots.api.Service
import org.github.telegabots.api.ServiceProvider

class ServiceProviderImpl : ServiceProvider {
    override fun <T : Service> getService(clazz: Class<T>): T? = null
}

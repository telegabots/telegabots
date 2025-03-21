package org.github.telegabots.api

interface ServiceProvider {
    fun <T : Service> getService(clazz: Class<T>): T?

    fun <T : UserService> getUserService(clazz: Class<T>, userId: Long): T?

    fun setInternalService(internalServiceProvider: ServiceProvider) {}
}

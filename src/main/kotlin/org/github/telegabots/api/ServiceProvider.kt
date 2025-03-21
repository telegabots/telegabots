package org.github.telegabots.api

/**
 * Provider for getting [Service] and [UserService] instances.
 *
 * Client should implement this interface for supporting client-specific services
 */
interface ServiceProvider {
    /**
     * Get [Service] by class
     */
    fun <T : Service> getService(clazz: Class<T>): T?

    /**
     * Get [UserService] by class and user id
     */
    fun <T : UserService> getUserService(clazz: Class<T>, userId: Long): T?
}

package org.github.telegabots.api

import java.util.function.Supplier

/**
 * Provider for getting [Service] and [UserService] instances.
 *
 * Client should implement this interface for supporting client-specific services
 */
interface ServiceProvider {
    /**
     * Get [Service] by class
     */
    fun <T : Service> getService(clazz: Class<T>): T? = null

    /**
     * Get [UserService] by class and user id
     */
    fun <T : UserService> getUserService(clazz: Class<T>, userId: Long): T? = null

    /**
     * Get [Supplier] by class
     */
    fun <T> getSupplier(clazz: Class<T>): Supplier<T>? = null
}

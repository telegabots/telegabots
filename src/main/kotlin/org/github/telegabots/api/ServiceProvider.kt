package org.github.telegabots.api

import java.util.function.Supplier

/**
 * Provider for getting [Service], [UserService] and [Supplier] instances.
 *
 * Client should implement this interface for supporting client-specific services
 */
interface ServiceProvider {
    /**
     * Get [Service] by class
     */
    fun <T : Service> getService(clazz: Class<T>): T =
        tryGetService(clazz) ?: error("Service not found: ${clazz.name}")

    /**
     * Get [Service] by class
     */
    fun <T : Service> tryGetService(clazz: Class<T>): T? = null

    /**
     * Get [UserService] by class and user id
     */
    fun <T : UserService> getUserService(clazz: Class<T>, userId: Long): T =
        tryGetUserService(clazz, userId) ?: error("User service not found: ${clazz.name}")

    /**
     * Get [UserService] by class and user id
     */
    fun <T : UserService> tryGetUserService(clazz: Class<T>, userId: Long): T? = null

    /**
     * Get [Supplier] by class
     */
    fun <T> getSupplier(clazz: Class<T>): Supplier<T> =
        tryGetSupplier(clazz) ?: error("Supplier<${clazz.name}> not found")

    /**
     * Get [Supplier] by class
     */
    fun <T> tryGetSupplier(clazz: Class<T>): Supplier<T>? = null
}

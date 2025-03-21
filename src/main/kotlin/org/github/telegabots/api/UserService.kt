package org.github.telegabots.api

/**
 * User related [Service]
 */
interface UserService : Service {
    /**
     * Returns user id
     */
    fun userId(): Long
}

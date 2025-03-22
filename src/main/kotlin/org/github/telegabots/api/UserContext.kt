package org.github.telegabots.api

/**
 * Current user context
 */
interface UserContext {
    fun isAdmin(): Boolean

    fun getUser(): InputUser
}

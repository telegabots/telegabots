package org.github.telegabots.api

interface UserContext {
    fun isAdmin(): Boolean

    fun user(): InputUser
}

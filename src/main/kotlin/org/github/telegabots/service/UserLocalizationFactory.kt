package org.github.telegabots.service

import org.github.telegabots.LocalizeProvider
import org.github.telegabots.Service

open class UserLocalizationFactory : Service {
    /**
     * Returns LocalizeProvider for user by id
     *
     * TODO: get current user language and returns specified LocalizeProvider
     */
    open fun getProvider(userId: Int): LocalizeProvider = DummyLocalizeProvider
}

private object DummyLocalizeProvider : LocalizeProvider {
    private val map = mapOf("GO_BACK" to "Back")

    override fun getString(key: String): String = map.getOrDefault(key, defaultValue = key)
}

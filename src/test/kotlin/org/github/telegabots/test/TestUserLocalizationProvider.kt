package org.github.telegabots.test

import org.github.telegabots.LocalizeProvider

class TestUserLocalizationProvider(vararg pairs: Pair<String, String>) : LocalizeProvider {
    private val map = pairs.toMap()

    override fun getString(key: String): String = map.getOrDefault(key, defaultValue = key)
}

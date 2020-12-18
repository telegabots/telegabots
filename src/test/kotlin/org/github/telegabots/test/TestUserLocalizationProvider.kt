package org.github.telegabots.test

import org.github.telegabots.LocalizeProvider
import org.slf4j.LoggerFactory

class TestUserLocalizationProvider(private val userId: Int) : LocalizeProvider {
    private val log = LoggerFactory.getLogger(TestUserLocalizationProvider::class.java)!!
    private val map = mutableMapOf<String, String>()

    init {
        log.info("TestUserLocalizationProvider created for user: $userId")
    }

    override fun getString(key: String): String = map.getOrDefault(key, defaultValue = key)

    fun addLocalization(vararg pairs: Pair<String, String>) {
        pairs.forEach { (key, value) -> map[key] = value }
    }
}

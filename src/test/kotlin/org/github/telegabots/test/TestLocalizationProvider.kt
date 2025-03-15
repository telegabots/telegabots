package org.github.telegabots.test

import org.github.telegabots.api.Language
import org.github.telegabots.api.LocalizeProvider
import org.github.telegabots.service.LanguageImpl
import org.slf4j.LoggerFactory

class TestLocalizationProvider() : LocalizeProvider {
    private val log = LoggerFactory.getLogger(TestLocalizationProvider::class.java)!!
    private val map = mutableMapOf<String, String>()

    init {
        log.info("TestUserLocalizationProvider created")
    }

    override fun language(): Language = LanguageImpl.ENGLISH

    override fun getString(key: String): String = map.getOrDefault(key, defaultValue = key)

    fun addLocalization(vararg pairs: Pair<String, String>) {
        pairs.forEach { (key, value) -> map[key] = value }
    }
}

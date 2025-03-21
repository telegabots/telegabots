package org.github.telegabots.test

import org.github.telegabots.api.Language
import org.github.telegabots.api.LocalizationProvider
import org.github.telegabots.service.LanguageImpl
import org.slf4j.LoggerFactory

class TestLocalizationProvider() : LocalizationProvider {
    private val log = LoggerFactory.getLogger(TestLocalizationProvider::class.java)!!
    private val map = mutableMapOf<String, String>()

    init {
        log.info("TestUserLocalizationProvider created")
    }

    override fun getLanguage(): Language = LanguageImpl.ENGLISH

    override fun getString(key: String): String = map.getOrDefault(key, defaultValue = key)

    fun addLocalization(vararg pairs: Pair<String, String>) {
        pairs.forEach { (key, value) -> map[key] = value }
    }
}

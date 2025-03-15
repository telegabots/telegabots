package org.github.telegabots.service

import org.github.telegabots.api.Language
import org.github.telegabots.api.LocalizationFactory
import org.github.telegabots.api.LocalizeProvider
import org.slf4j.LoggerFactory

open class FileBasedLocalizationFactory(
    val jsonService: JsonService,
    val file: String = "telegabots-locales.json"
) : LocalizationFactory {
    internal val locales: Map<Language, LocalizeProvider>

    init {
        locales = loadLocales()
    }

    override fun getSupportedLanguages(): List<Language> = locales.keys.toList()

    override fun getProvider(langCode: String): LocalizeProvider =
        locales[LanguageImpl.valueOf(langCode)] ?: DummyLocalizeProvider

    override fun getLanguage(langCode: String): Language? = getSupportedLanguages().firstOrNull { it.code() == langCode }

    private fun loadLocales(): Map<Language, LocalizeProvider> {
        try {
            val fileRef = javaClass.classLoader.getResourceAsStream(file)

            if (fileRef == null) {
                log.warn("Localization file not found: {}", file)
                return emptyMap()
            }

            val root = jsonService.parse(fileRef.bufferedReader(Charsets.UTF_8).readText(), FileRoot::class.java)
            return root.locales
                .map { LanguageImpl.valueOf(it.lang) to it.items }
                .filter { it.first != null }
                .associate { it.first!! to MapLocalizeProvider(it.first!!, it.second) }
        } catch (e: Exception) {
            throw IllegalStateException("Localization file parsing failed: ${e.message}, file: $file", e)
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(FileBasedLocalizationFactory::class.java)!!
    }
}

private class MapLocalizeProvider(val language: Language, val map: Map<String, String>) : LocalizeProvider {
    override fun language(): Language = language

    override fun getString(key: String): String = map.getOrDefault(key, defaultValue = key)
}

private object DummyLocalizeProvider : LocalizeProvider {
    override fun language(): Language = LanguageImpl.DUMMY

    override fun getString(key: String): String = key
}

/**
 * Format defining class
 */
private data class FileRoot(val locales: List<Local>)

private data class Local(val lang: String, val items: Map<String, String>)



package org.github.telegabots.service

import org.github.telegabots.api.Language
import org.github.telegabots.api.LocalizationFactory
import org.github.telegabots.api.LocalizeProvider
import org.slf4j.LoggerFactory

open class FileBasedLocalizationFactory(
    val jsonService: JsonService,
    val file: String = "telegabots-locales.json",
    val defaultFile: String = "telegabots-locales-default.json"
) : LocalizationFactory {
    internal val locales: Map<Language, LocalizeProvider>

    init {
        val userLocales = loadLocales(file)
        val defaultLocales = loadLocales(defaultFile)
        locales = merge(defaultLocales, userLocales)
    }

    override fun getSupportedLanguages(): List<Language> = locales.keys.toList()

    override fun getProvider(langCode: String): LocalizeProvider =
        locales[LanguageImpl.valueOf(langCode)] ?: DummyLocalizeProvider

    private fun loadLocales(fileName: String): Map<Language, LocalizeProvider> {
        try {
            val fileRef = javaClass.classLoader.getResourceAsStream(fileName)

            if (fileRef == null) {
                log.warn("Localization file not found: {}. Please, define if it", fileName)
                return emptyMap()
            }

            val root = jsonService.parse(fileRef.bufferedReader(Charsets.UTF_8).readText(), FileRoot::class.java)
            return root.locales
                .map { LanguageImpl.valueOf(it.lang) to it.items }
                .filter { it.first != null }
                .associate { it.first!! to MapLocalizeProvider(it.first!!, it.second) }
        } catch (e: Exception) {
            throw IllegalStateException("Localization file parsing failed: ${e.message}, file: $fileName", e)
        }
    }

    private fun merge(
        defaultLocales: Map<Language, LocalizeProvider>,
        userLocales: Map<Language, LocalizeProvider>
    ): Map<Language, LocalizeProvider> {
        return userLocales.map { it.key to (it.value as MapLocalizeProvider).merge(defaultLocales[it.key]) }.toMap()
    }

    private companion object {
        val log = LoggerFactory.getLogger(FileBasedLocalizationFactory::class.java)!!
    }
}

private class MapLocalizeProvider(val language: Language, val map: Map<String, String>) : LocalizeProvider {
    override fun language(): Language = language

    override fun getString(key: String): String = map.getOrDefault(key, defaultValue = key)

    fun merge(defaultProvider: LocalizeProvider?): MapLocalizeProvider {
        if (defaultProvider is MapLocalizeProvider) {
            val newMap = defaultProvider.map.toMutableMap()
            newMap.putAll(map)
            return MapLocalizeProvider(language, newMap)
        }
        return this
    }
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



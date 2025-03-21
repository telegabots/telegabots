package org.github.telegabots.service

import org.github.telegabots.api.Language
import org.github.telegabots.api.LocalizationFactory
import org.github.telegabots.api.LocalizationProvider
import org.slf4j.LoggerFactory

open class FileBasedLocalizationFactory(
    val jsonService: JsonService,
    val file: String = "telegabots-locales.json",
    val defaultFile: String = "telegabots-locales-default.json"
) : LocalizationFactory {
    internal val locales: Map<Language, LocalizationProvider>

    init {
        val userLocales = loadLocales(file)
        val defaultLocales = loadLocales(defaultFile)
        locales = merge(defaultLocales, userLocales)
    }

    override fun getSupportedLanguages(): List<Language> = locales.keys.toList()

    override fun getProvider(langCode: String): LocalizationProvider =
        locales[LanguageImpl.valueOf(langCode)] ?: DummyLocalizationProvider

    private fun loadLocales(fileName: String): Map<Language, LocalizationProvider> {
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
                .associate { it.first!! to MapLocalizationProvider(it.first!!, it.second) }
        } catch (e: Exception) {
            throw IllegalStateException("Localization file parsing failed: ${e.message}, file: $fileName", e)
        }
    }

    private fun merge(
        defaultLocales: Map<Language, LocalizationProvider>,
        userLocales: Map<Language, LocalizationProvider>
    ): Map<Language, LocalizationProvider> {
        return userLocales.map { it.key to (it.value as MapLocalizationProvider).merge(defaultLocales[it.key]) }.toMap()
    }

    private companion object {
        val log = LoggerFactory.getLogger(FileBasedLocalizationFactory::class.java)!!
    }
}

private class MapLocalizationProvider(private val language: Language, val map: Map<String, String>) : LocalizationProvider {
    override fun getLanguage(): Language = language

    override fun getString(key: String): String = map.getOrDefault(key, defaultValue = key)

    fun merge(defaultProvider: LocalizationProvider?): MapLocalizationProvider {
        if (defaultProvider is MapLocalizationProvider) {
            val newMap = defaultProvider.map.toMutableMap()
            newMap.putAll(map)
            return MapLocalizationProvider(language, newMap)
        }
        return this
    }
}

private object DummyLocalizationProvider : LocalizationProvider {
    override fun getLanguage(): Language = LanguageImpl.DUMMY

    override fun getString(key: String): String = key
}

/**
 * Format defining class
 */
private data class FileRoot(val locales: List<Local>)

private data class Local(val lang: String, val items: Map<String, String>)



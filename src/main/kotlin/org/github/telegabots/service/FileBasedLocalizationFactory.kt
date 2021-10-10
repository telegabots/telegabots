package org.github.telegabots.service

import org.github.telegabots.api.LocalizeProvider
import org.github.telegabots.api.UserLocalizationFactory
import org.slf4j.LoggerFactory
import java.util.*

open class FileBasedLocalizationFactory(
    val jsonService: JsonService,
    val file: String = "telegabots-locales.json"
) : UserLocalizationFactory {
    private val locales: Map<String, LocalizeProvider>

    init {
        locales = loadLocales()
    }

    /**
     * Returns LocalizeProvider for user by id
     */
    override fun getProvider(userId: Long): LocalizeProvider = locales[getLangByUser(userId)] ?: DummyLocalizeProvider

    private fun getLangByUser(userId: Long): String {
        // TODO: get current user language and returns specified LocalizeProvider
        return Locale.ENGLISH.language
    }

    private fun loadLocales(): Map<String, LocalizeProvider> {
        try {
            val fileRef = javaClass.classLoader.getResourceAsStream(file)

            if (fileRef == null) {
                log.warn("Localization file not found: {}", file)
                return emptyMap()
            }

            val root = jsonService.parse(fileRef.bufferedReader(Charsets.UTF_8).readText(), FileRoot::class.java)
            return root.locales.associate { it.lang to MapLocalizeProvider(it.lang, it.items) }
        } catch (e: Exception) {
            throw IllegalStateException("Localization file parsing failed: ${e.message}, file: $file", e)
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(FileBasedLocalizationFactory::class.java)!!
    }
}

private class MapLocalizeProvider(val language: String, val map: Map<String, String>) : LocalizeProvider {
    override fun language(): String = language

    override fun getString(key: String): String = map.getOrDefault(key, defaultValue = key)
}

private object DummyLocalizeProvider : LocalizeProvider {
    override fun language(): String = "default"

    override fun getString(key: String): String = key
}

/**
 * Format defining class
 */
private data class FileRoot(val locales: List<Local>)

private data class Local(val lang: String, val items: Map<String, String>)

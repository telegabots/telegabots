package org.github.telegabots.service

import org.github.telegabots.api.LocalizeProvider
import org.github.telegabots.api.UserLocalizationFactory
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
    override fun getProvider(userId: Int): LocalizeProvider = locales[getLangByUser(userId)] ?: DummyLocalizeProvider

    private fun getLangByUser(userId: Int): String {
        // TODO: get current user language and returns specified LocalizeProvider
        return Locale.ENGLISH.language
    }

    private fun loadLocales(): Map<String, LocalizeProvider> {
        try {
            val fileRef =
                javaClass.classLoader.getResourceAsStream(file) ?: throw IllegalStateException("Resource not found")
            val root = jsonService.parse(fileRef.bufferedReader(Charsets.UTF_8).readText(), FileRoot::class.java)

            return root.locales.map { it.lang to MapLocalizeProvider(it.lang, it.items) }.toMap()
        } catch (e: Exception) {
            throw IllegalStateException("Localization file parsing failed: ${e.message}, file: $file", e)
        }
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

package org.github.telegabots.api

/**
 * Factory of getting [LocalizationProvider]
 */
interface LocalizationFactory : Service {
    fun getSupportedLanguages(): List<Language>

    /**
     * Returns [LocalizationProvider] by language code
     */
    fun getProvider(langCode: String): LocalizationProvider

    /**
     * Returns Language by language code from supported languages
     */
    fun findLanguage(langCode: String): Language? =  getSupportedLanguages().firstOrNull { it.code() == langCode }
}

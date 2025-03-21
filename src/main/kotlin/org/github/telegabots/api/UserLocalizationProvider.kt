package org.github.telegabots.api

/**
 * User related Localization provider
 */
interface UserLocalizationProvider : LocalizationProvider, UserService {
    /**
     * Sets language for the user
     */
    fun setLanguage(language: Language)

    /**
     * Returns all supported languages
     */
    fun getSupportedLanguages(): List<Language>

    /**
     * Returns language by language code from supported languages
     */
    fun findLanguage(langCode: String): Language? =  getSupportedLanguages().firstOrNull { it.code() == langCode }
}

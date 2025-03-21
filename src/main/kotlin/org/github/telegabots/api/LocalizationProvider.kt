package org.github.telegabots.api

/**
 * Specific language related Localization provider
 */
interface LocalizationProvider {
    /**
     * Language
     */
    fun getLanguage(): Language

    /**
     * Returns localized string of key itself
     */
    fun getString(key: String): String
}

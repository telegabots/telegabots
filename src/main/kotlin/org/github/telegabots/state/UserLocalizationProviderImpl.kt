package org.github.telegabots.state

import org.github.telegabots.api.Language
import org.github.telegabots.api.LocalizationFactory
import org.github.telegabots.api.UserLocalizationProvider

/**
 * Implementation of [UserLocalizationProvider]
 */
internal class UserLocalizationProviderImpl(
    private val localizationFactory: LocalizationFactory,
    private val userSettingsService: UserSettingsService
) : UserLocalizationProvider {
    @Volatile
    private var language: Language? = null

    override fun userId(): Long = userSettingsService.userId()

    override fun getSupportedLanguages(): List<Language> = localizationFactory.getSupportedLanguages()

    override fun getString(key: String): String =
        localizationFactory.getProvider(getLanguage().code()).getString(key)

    override fun getLanguage(): Language {
        if (language == null) {
            val userSettings = userSettingsService.getSettings()
            if (userSettings.langCode != null) {
                language = getSupportedLanguages().firstOrNull { it.code() == userSettings.langCode }
            }
        }

        return language ?: getSupportedLanguages().first()
    }

    override fun setLanguage(language: Language) {
        userSettingsService.applySettings({ it.copy(langCode = language.code()) })
        this.language = language
    }
}

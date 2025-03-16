package org.github.telegabots.state

import org.github.telegabots.api.Language
import org.github.telegabots.api.LocalizationFactory
import org.github.telegabots.api.UserLanguageService

/**
 * Implementation of [UserLanguageService]
 */
internal class UserLanguageServiceImpl(
    private val localizationFactory: LocalizationFactory,
    private val userSettingsService: UserSettingsService
) : UserLanguageService {
    private var language: Language? = null

    override fun userId(): Long = userSettingsService.userId()

    override fun getSupportedLanguages(): List<Language>  = localizationFactory.getSupportedLanguages()

    override fun getLanguage(): Language {
        val supportedLanguages = localizationFactory.getSupportedLanguages()
        if (language == null) {
            val userSettings = userSettingsService.getSettings()
            if (userSettings.langCode != null) {
                language = supportedLanguages.firstOrNull { it.code() == userSettings.langCode }
            }
        }

        return language ?: supportedLanguages.first()
    }

    override fun setLanguage(language: Language) {
        userSettingsService.applySettings({ it.copy(langCode = language.code()) })
        this.language = language
    }
}

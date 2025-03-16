package org.github.telegabots.std.cmd

import org.github.telegabots.api.*
import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler

/**
 * Show all supported languages and allow user to change current language
 */
open class LanguageCommand : BaseCommand() {
    @TextHandler
    fun handle(message: String, languageService: UserLanguageService) {
        showLanguagesPage(languageService, false)
    }

    @InlineHandler
    fun handleInline(langCode: String, languageService: UserLanguageService) {
        val newLanguage = languageService.findLanguage(langCode)
        if (newLanguage != null && languageService.getLanguage() !== newLanguage) {
            languageService.setLanguage(newLanguage)
        }

        showLanguagesPage(languageService, true)
    }

    private fun showLanguagesPage(
        languageService: UserLanguageService,
        isUpdate: Boolean
    ) {
        val currLanguage = languageService.getLanguage()
        val subCommands: List<List<SubCommand>> = languageService.getSupportedLanguages()
            .map { lang -> SubCommand.of(lang.code(), getTitle(lang, currLanguage === lang)) }
            .map { listOf(it) }

        // TODO: support text of different languages
        val pageBuilder = context.page("Current language: " + currLanguage.name())
            .messageType(MessageType.Inline)
            .enableBack()
            .subCommands(subCommands)

        if (isUpdate) {
            pageBuilder.update()
        } else {
            pageBuilder.create()
        }
    }

    private fun getTitle(lang: Language, isCurrent: Boolean): String {
        return (lang.flag() + lang.name()) + (if (isCurrent) " ✅" else "")
    }
}

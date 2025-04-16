package org.github.telegabots.std.cmd

import org.github.telegabots.api.*
import org.github.telegabots.api.annotation.InlineHandler

/**
 * Shows all supported languages and allow user to change current [Language]
 */
open class LanguageCommand : BaseCommand() {
    @InlineHandler
    fun handleInline(langCode: String, provider: UserLocalizationProvider) {
        val newLanguage = provider.findLanguage(langCode)
        if (newLanguage != null && provider.getLanguage() !== newLanguage) {
            provider.setLanguage(newLanguage)
        }

        val currLanguage = provider.getLanguage()
        val subCommands: List<List<SubCommand>> = provider.getSupportedLanguages()
            .map { lang -> SubCommand.of(lang.code(), getTitle(lang, currLanguage === lang)) }
            .map { listOf(it) }

        context.page(String.format(provider.getString("LANGUAGE_CURRENT"), currLanguage.nativeName()))
            .contentType(ContentType.Markdown)
            .messageType(MessageType.Inline)
            .enableBack()
            .subCommands(subCommands)
            .update()
    }

    private fun getTitle(lang: Language, isCurrent: Boolean): String {
        return (lang.flag() + lang.nativeName()) + (if (isCurrent) " ✅" else "")
    }
}

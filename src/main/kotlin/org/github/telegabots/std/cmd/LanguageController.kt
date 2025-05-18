package org.github.telegabots.std.cmd

import org.github.telegabots.api.*
import org.github.telegabots.api.annotation.InlineHandler

/**
 * Shows all supported languages and allow user to change current [Language]
 */
open class LanguageController : BaseController() {
    @InlineHandler
    fun handleInline(langCode: String, provider: UserLocalizationProvider) {
        val newLanguage = provider.findLanguage(langCode)
        if (newLanguage != null && provider.getLanguage() !== newLanguage) {
            provider.setLanguage(newLanguage)
        }

        val currLanguage = provider.getLanguage()
        val buttons: List<List<Button>> = provider.getSupportedLanguages()
            .map { lang -> Button.of(lang.code(), getTitle(lang, currLanguage === lang)) }
            .map { listOf(it) }

        context.page(String.format(provider.getString("LANGUAGE_CURRENT"), currLanguage.nativeName()))
            .contentType(ContentType.Markdown)
            .messageType(MessageType.Inline)
            .enableBack()
            .buttons(buttons)
            .update()
    }

    private fun getTitle(lang: Language, isCurrent: Boolean): String {
        return (lang.flag() + lang.nativeName()) + (if (isCurrent) " ✅" else "")
    }
}

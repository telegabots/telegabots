package org.github.telegabots.service

import org.github.telegabots.api.Language

internal data class LanguageImpl(
    private val code: String,
    private val name: String,
    private val nativeName: String,
    private val flag: String
) : Language {
    override fun code(): String = code

    override fun name(): String = name

    override fun nativeName(): String = nativeName

    override fun flag(): String = flag

    override fun toString(): String = "$code - $name ($nativeName)"

    companion object {
        fun valueOf(code: String): Language? {
            return when (code) {
                "en" -> return ENGLISH
                "ru" -> return RUSSIAN
                "de" -> return GERMAN
                "uk" -> return UKRAINIAN
                else -> null
            }
        }

        val DUMMY = LanguageImpl("dummy", "Dummy", "Dummy", "")

        val ENGLISH = LanguageImpl("en", "English", "English", "🇬🇧")

        val GERMAN = LanguageImpl("de", "German", "Deutsch", "🇩🇪")

        val RUSSIAN = LanguageImpl("ru", "Russian", "Русский", "🇷🇺")

        val UKRAINIAN = LanguageImpl("uk", "Ukrainian", "Українська", "🇺🇦")
    }
}
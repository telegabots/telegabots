package org.github.telegabots.api

/**
 * Information about language
 */
interface Language {
    /**
     * Language code. For example: en, de, etc.
     */
    fun code(): String

    /**
     * Language name. For example: English, German, etc.
     */
    fun name(): String

    /**
     * Native language name. For example: English, Deutsch, etc.
     */
    fun nativeName(): String

    /**
     * Flag emoji
     */
    fun flag(): String
}

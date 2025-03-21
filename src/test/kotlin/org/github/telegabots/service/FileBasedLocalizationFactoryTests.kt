package org.github.telegabots.service

import org.github.telegabots.api.SystemCommands
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertSame

class FileBasedLocalizationFactoryTests {
    @Test
    fun testFactoryWhenResourceNotFound() {
        val factory = FileBasedLocalizationFactory(
            jsonService = JsonService(),
            file = "file-xyz.json"
        )

        assertEquals(0, factory.locales.size)

        val provider = factory.getProvider("xx")
        assertEquals("dummy", provider.getLanguage().code())

        val key = UUID.randomUUID().toString()
        assertEquals(key, provider.getString(key))
        assertEquals(provider.javaClass.name, "org.github.telegabots.service.DummyLocalizationProvider")
    }

    @Test
    fun testSupportedLanguages() {
        val factory = FileBasedLocalizationFactory(jsonService = JsonService())
        val supportedLanguages = factory.getSupportedLanguages()
        assertEquals(listOf("en", "de"), supportedLanguages.map { it.code() })
        { "Only languages from telegabots-locales.json should be supported" }
        val providerEng = factory.getProvider("en")

        assertSame(LanguageImpl.ENGLISH, providerEng.getLanguage())
        assertEquals("Back", providerEng.getString(SystemCommands.GO_BACK))
        assertEquals("Yes", providerEng.getString("_YES"))
        assertEquals("No", providerEng.getString("_NO"))
        assertEquals("This is custom key", providerEng.getString("CUSTOM_KEY"))

        val providerGer = factory.getProvider("de")
        assertSame(LanguageImpl.GERMAN, providerGer.getLanguage())
        assertEquals("Zurück", providerGer.getString(SystemCommands.GO_BACK))
        assertEquals("Ja!!!", providerGer.getString("_YES"), "Custom key should override standard key")
        assertEquals("Dies ist ein benutzerdefinierter Schlüssel", providerGer.getString("CUSTOM_KEY"))
    }
}

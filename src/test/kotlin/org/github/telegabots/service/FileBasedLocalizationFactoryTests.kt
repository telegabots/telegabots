package org.github.telegabots.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class FileBasedLocalizationFactoryTests {
    @Test
    fun testFactoryWhenResourceNotFound() {
        val factory = FileBasedLocalizationFactory(
            jsonService = JsonService(),
            file = "file-xyz.json"
        )

        assertEquals(0, factory.locales.size)

        val provider = factory.getProvider("xx")
        assertEquals("dummy", provider.language().code())

        val key = UUID.randomUUID().toString()
        assertEquals(key, provider.getString(key))
        assertEquals(provider.javaClass.name, "org.github.telegabots.service.DummyLocalizeProvider")
    }

    @Test
    fun testLoadDefaultLocale() {
        val factory = FileBasedLocalizationFactory(jsonService = JsonService())
        val provider = factory.getProvider("en")
        val language = provider.language()

        assertEquals("en", language.code())
        assertEquals("English", language.name())
        assertEquals("English", language.nativeName())
        assertEquals("Back", provider.getString("_BACK"))
    }
}

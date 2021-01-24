package org.github.telegabots.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalStateException

class FileBasedLocalizationFactoryTests {
    @Test
    fun testFail_WhenResourceNotFound() {
        val ex = assertThrows<IllegalStateException> {
            FileBasedLocalizationFactory(
                jsonService = JsonService(),
                file = "file-xyz.json"
            )
        }

        assertEquals("Localization file parsing failed: Resource not found, file: file-xyz.json", ex.message)
    }

    @Test
    fun testLoadDefaultLocale() {
        val factory = FileBasedLocalizationFactory(jsonService = JsonService())
        val provider = factory.getProvider(123)

        assertEquals("en", provider.language())
        assertEquals("Back", provider.getString("_BACK"))
    }
}

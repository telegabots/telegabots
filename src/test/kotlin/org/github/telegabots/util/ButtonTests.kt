package org.github.telegabots.util

import org.github.telegabots.api.BaseController
import org.github.telegabots.api.StateRef
import org.github.telegabots.api.Button
import org.github.telegabots.api.annotation.TextHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Tests for [Button]
 */
class ButtonTests {
    @Test
    fun testTitleIdByControllerClass() {
        assertEquals("FOO_BAR", Button.titleIdOf(FooBarController::class.java))
    }

    @Test
    fun testTitleIdByControllerWithDigits() {
        assertEquals("FOO_BAR_CONTROLLER1", Button.titleIdOf(FooBarController1::class.java))
        assertEquals("FOO_BAR1", Button.titleIdOf(FooBar1Controller::class.java))
    }

    @Test
    fun testCreateSubControllerByControllerClass() {
        val expected = Button(handler = FooBarController::class.java, titleId = "FOO_BAR", state = null)

        assertEquals(expected, Button.of(FooBarController::class.java))
        assertEquals(expected, Button.of(FooBarController::class.java, titleId = "FOO_BAR"))
        assertEquals(expected, Button.of<FooBarController>())
        assertEquals(expected, Button.of<FooBarController>(titleId = "FOO_BAR"))
    }

    @Test
    fun testCreateButtonByControllerClassWithEmptyState() {
        val expected = Button(handler = FooBarController::class.java, titleId = "FOO_BAR", state = StateRef.Empty)

        assertEquals(expected, Button.of(FooBarController::class.java, StateRef.Empty))
        assertEquals(expected, Button.of(FooBarController::class.java, titleId = "FOO_BAR", state = StateRef.Empty))
        assertEquals(expected, Button.of<FooBarController>(state = StateRef.Empty))
        assertEquals(expected, Button.of<FooBarController>(titleId = "FOO_BAR", state = StateRef.Empty))
    }

    @Test
    fun testCreateButtonByControllerClassAndCustomerTitleId() {
        val expected = Button(handler = FooBarController::class.java, titleId = "SOME_ANOTHER_ID", state = null)

        assertEquals(expected, Button.of(FooBarController::class.java, titleId = "SOME_ANOTHER_ID"))
    }

    @Test
    fun testFailsWhenTitleIdLengthInvalid() {
        val ex = assertThrows<IllegalStateException> { Button.of("") }
        assertEquals("TitleId is empty. Expected length is [1..64] in bytes", ex.message)

        val titleId64 = "ü12345678901234567890123456789012345678901234567890123456789012"
        assertEquals(63, titleId64.length)
        assertEquals(64, titleId64.toByteArray().size)
        Button.of(titleId64) // valid

        val ex2 = assertThrows<IllegalStateException> { Button.of(titleId64 + "0") }
        assertEquals(
            "TitleId is too long: 65. Expected length is [1..64] in bytes. TitleId: ${titleId64}0",
            ex2.message
        )
    }
}

internal class FooBarController : TestBaseClass()

internal class FooBarController1 : TestBaseClass()

internal class FooBar1Controller : TestBaseClass()

internal abstract class TestBaseClass : BaseController() {
    @TextHandler
    fun handle(msg: String) {
    }
}

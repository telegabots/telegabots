package org.github.telegabots.java

import org.github.telegabots.BaseTests
import org.github.telegabots.test.CommandAssert.assertNotCalled
import org.github.telegabots.test.CommandAssert.assertWasCalled
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class JavaCommandTests : BaseTests() {
    @Test
    fun testCommand_Success_JavaCommandCall() {
        val executor = createExecutor(JavaSimpleCommand::class.java)
        val update = createAnyMessage()

        assertNotCalled<JavaSimpleCommand>()

        val success = executor.handle(update)

        assertWasCalled<JavaSimpleCommand>()
        assertTrue(success)
    }
}

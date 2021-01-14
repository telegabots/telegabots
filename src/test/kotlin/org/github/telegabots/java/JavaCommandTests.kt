package org.github.telegabots.java

import org.github.telegabots.BaseTests
import org.github.telegabots.test.scenario
import org.junit.jupiter.api.Test

class JavaCommandTests : BaseTests() {
    @Test
    fun testCommand_Success_JavaCommandCall() {
        scenario<JavaSimpleCommand> {
            assertThat {
                rootNotCalled()
            }

            user {
                sendTextMessage("Command in java)")
            }

            assertThat {
                rootWasCalled(1)
                commandReturnTrue()
            }
        }
    }
}

package org.github.telegabots.java

import org.github.telegabots.BaseTests
import org.github.telegabots.test.scenario
import org.junit.jupiter.api.Test

class JavaControllerTests : BaseTests() {
    @Test
    fun testController_Success_JavaControllerCall() {
        scenario<JavaSimpleController> {
            assertThat {
                rootNotCalled()
            }

            user {
                sendTextMessage("Controller in java)")
            }

            assertThat {
                rootWasCalled(1)
                controllerReturnTrue()
            }
        }
    }
}

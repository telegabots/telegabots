package org.github.telegabots.util

import org.github.telegabots.CODE_NOT_REACHED
import org.github.telegabots.api.BaseController
import org.github.telegabots.api.EmptyController
import org.github.telegabots.api.ServiceProvider
import org.github.telegabots.api.annotation.TextHandler
import org.github.telegabots.controllers.AbstractBaseController
import org.github.telegabots.handler.*
import org.github.telegabots.java.JavaSimpleController
import org.github.telegabots.service.ControllerHandlers
import org.github.telegabots.test.mockStrict
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Tests for [ControllerValidatorImpl]
 */
class ControllerValidatorTests() {
    private val serviceProviderMock = mockStrict(ServiceProvider::class.java)
    private val controllerHandlers = ControllerHandlers(serviceProvider = serviceProviderMock)
    private val controllerValidator = ControllerValidatorImpl(controllerHandlers)

    @Test
    fun testValidateAll() {
        controllerValidator.validateAll("org.examples.testbot.controllers")
    }

    @Test
    fun testValidateSuccess() {
        controllerValidator.validate(
            InvalidControllerTextMessageAfterInline::class.java,
            InvalidControllerInlineMessageAfterText::class.java,
            ControllerWithOnlyInlineHandler::class.java,
            ValidationTextHandlerRootController::class.java,
            TestController::class.java,
            JavaSimpleController::class.java,
            InvalidRootControllerWithoutTextHandler::class.java,
            TextControllerAddingPage::class.java,
            SubMenu2Controller::class.java,
            InheritSimpleController::class.java,
            ControllerContextHolder::class.java,
            ControllerRoot::class.java,
            FooBarController1::class.java,
            InlineControllerAddingPage::class.java,
            SimpleControllerReturnsVoid::class.java,
            SimpleControllerThrowsError::class.java,
            ControllerUsesControllerContext::class.java,
            ValidInlineControllerStringInt::class.java,
            SubMenu1Controller::class.java,
            ValidationInlineHandlerRootController::class.java,
            ControllerWithReadonlyLocalState::class.java,
            FooBar1Controller::class.java,
            ControllerWithStateParam::class.java,
            FooBarController::class.java,
            ControllerWithServiceParam::class.java,
            EmptyController::class.java,
            SimpleControllerReturnsBool::class.java,
            ValidInlineControllerWithTwoStringParams::class.java,
            ControllerWithOnlyTextHandler::class.java,
            AnotherController::class.java
        )
    }

    @Test
    fun testValidateTextController_Fail_WithoutAtLeastOneParam() {
        val ex = assertThrows<IllegalStateException> { controllerValidator.validate(InvalidControllerWithoutAnyParam::class.java) }

        assertEquals("Handler must contains at least one parameter: public final boolean org.github.telegabots.handler.InvalidControllerWithoutAnyParam.execute()", ex.message)
    }

    @Test
    fun testValidateInlineController_Fail_WithoutAtLeastOneParam() {
        val ex = assertThrows<IllegalStateException> { controllerValidator.validate(InvalidInlineControllerWithoutAnyParam::class.java) }

        assertEquals("Handler must contains at least one parameter: public final void org.github.telegabots.handler.InvalidInlineControllerWithoutAnyParam.handle()", ex.message)
    }

    @Test
    fun testValidateTextController_Fail_WithoutReturnBool() {
        val ex = assertThrows<IllegalStateException> { controllerValidator.validate(InvalidControllerReturnNonBoolParam::class.java) }

        assertEquals("Handler must return bool or void but it returns int in method public final int org.github.telegabots.handler.InvalidControllerReturnNonBoolParam.execute(java.lang.String)", ex.message)
    }

    @Test
    fun testValidateInlineController_Fail_WithoutFirstStringParam() {
        val ex = assertThrows<IllegalStateException> { controllerValidator.validate(InvalidInlineControllerWithTwoIntParams::class.java) }
        val ex2 = assertThrows<IllegalStateException> { controllerValidator.validate(InvalidInlineControllerWithOnlyIntParam::class.java) }
        val ex3 = assertThrows<IllegalStateException> { controllerValidator.validate(InvalidControllerWithoutStringParam::class.java) }

        assertEquals("First parameter must be String but found int in handler public final void org.github.telegabots.handler.InvalidInlineControllerWithTwoIntParams.handle(int,int)", ex.message)
        assertEquals("First parameter must be String but found int in handler public final void org.github.telegabots.handler.InvalidInlineControllerWithOnlyIntParam.handle(int)", ex2.message)
        assertEquals("First parameter must be String but found int in handler public final void org.github.telegabots.handler.InvalidControllerWithoutStringParam.execute(int)", ex3.message)
    }

    @Test
    fun testValidate_Fail_WithControllerContextInParam() {
        val ex = assertThrows<IllegalStateException> { controllerValidator.validate(ControllerWithControllerContextParam::class.java) }

        assertEquals("ControllerContext can not be used as handler parameter. Use \"context\" field instead. Handler: public final void org.github.telegabots.handler.ControllerWithControllerContextParam.handle(java.lang.String,org.github.telegabots.api.ControllerContext)", ex.message)
    }

    @Test
    fun testValidate_Fail_WhenControllerWithoutDefaultConstructor() {
        val ex = assertThrows<IllegalStateException> { controllerValidator.validate(ControllerWithoutDefaultConstructor::class.java) }

        assertEquals("Argument type not implements Service: class java.lang.String. Controller: org.github.telegabots.util.ControllerWithoutDefaultConstructor", ex.message)
    }

    @Test
    fun testValidate_Fail_WhenControllerIsAbstract() {
        val ex = assertThrows<IllegalStateException> { controllerValidator.validate(AbstractBaseController::class.java) }

        assertEquals("Controller class cannot be created: org.github.telegabots.controllers.AbstractBaseController", ex.message)
    }
}

internal class ControllerWithoutDefaultConstructor(val someVal: String) : BaseController() {
    @TextHandler
    fun handle(msg: String) {
        CODE_NOT_REACHED()
    }
}

package org.github.telegabots.util

import org.github.telegabots.api.EmptyCommand
import org.github.telegabots.handler.*
import org.github.telegabots.handler.InvalidCommandInlineMessageAfterText
import org.github.telegabots.handler.InvalidCommandTextMessageAfterInline
import org.github.telegabots.handler.InvalidRootCommandWithoutTextHandler
import org.github.telegabots.java.JavaSimpleCommand
import org.github.telegabots.service.CommandHandlers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalStateException

class CommandValidatorTests() {
    private val commandHandlers = CommandHandlers()
    private val commandValidator = CommandValidatorImpl(commandHandlers)

    @Test
    fun testValidateAll() {
        commandValidator.validateAll("org.examples.allfeaturedbot.commands")
    }

    @Test
    fun testValidateSuccess() {
        commandValidator.validate(
            InvalidCommandTextMessageAfterInline::class.java,
            InvalidCommandInlineMessageAfterText::class.java,
            CommandWithOnlyInlineHandler::class.java,
            ValidationTextHandlerRootCommand::class.java,
            TestCommand::class.java,
            JavaSimpleCommand::class.java,
            InvalidRootCommandWithoutTextHandler::class.java,
            TextCommandAddingPage::class.java,
            SubMenu2Command::class.java,
            InheritSimpleCommand::class.java,
            CommandContextHolder::class.java,
            CommandRoot::class.java,
            FooBarCommand1::class.java,
            InlineCommandAddingPage::class.java,
            SimpleCommandReturnsVoid::class.java,
            SimpleCommandThrowsError::class.java,
            CommandUsesCommandContext::class.java,
            ValidInlineCommandStringInt::class.java,
            SubMenu1Command::class.java,
            ValidationInlineHandlerRootCommand::class.java,
            CommandWithReadonlyLocalState::class.java,
            FooBar1Command::class.java,
            CommandWithStateParam::class.java,
            FooBarCommand::class.java,
            CommandWithServiceParam::class.java,
            EmptyCommand::class.java,
            SimpleCommandReturnsBool::class.java,
            ValidInlineCommandWithTwoStringParams::class.java,
            CommandWithOnlyTextHandler::class.java,
            AnotherCommand::class.java
        )
    }

    @Test
    fun testValidateTextCommand_Fail_WithoutAtLeastOneParam() {
        val ex = assertThrows<IllegalStateException> { commandValidator.validate(InvalidCommandWithoutAnyParam::class.java) }

        assertEquals("Handler must contains at least one parameter: public final boolean org.github.telegabots.handler.InvalidCommandWithoutAnyParam.execute()", ex.message)
    }

    @Test
    fun testValidateInlineCommand_Fail_WithoutAtLeastOneParam() {
        val ex = assertThrows<IllegalStateException> { commandValidator.validate(InvalidInlineCommandWithoutAnyParam::class.java) }

        assertEquals("Handler must contains at least one parameter: public final void org.github.telegabots.handler.InvalidInlineCommandWithoutAnyParam.handle()", ex.message)
    }

    @Test
    fun testValidateTextCommand_Fail_WithoutReturnBool() {
        val ex = assertThrows<IllegalStateException> { commandValidator.validate(InvalidCommandReturnNonBoolParam::class.java) }

        assertEquals("Handler must return bool or void but it returns int in method public final int org.github.telegabots.handler.InvalidCommandReturnNonBoolParam.execute(java.lang.String)", ex.message)
    }

    @Test
    fun testValidateInlineCommand_Fail_WithoutFirstStringParam() {
        val ex = assertThrows<IllegalStateException> { commandValidator.validate(InvalidInlineCommandWithTwoIntParams::class.java) }
        val ex2 = assertThrows<IllegalStateException> { commandValidator.validate(InvalidInlineCommandWithOnlyIntParam::class.java) }
        val ex3 = assertThrows<IllegalStateException> { commandValidator.validate(InvalidCommandWithoutStringParam::class.java) }

        assertEquals("First parameter must be String but found int in handler public final void org.github.telegabots.handler.InvalidInlineCommandWithTwoIntParams.handle(int,int)", ex.message)
        assertEquals("First parameter must be String but found int in handler public final void org.github.telegabots.handler.InvalidInlineCommandWithOnlyIntParam.handle(int)", ex2.message)
        assertEquals("First parameter must be String but found int in handler public final void org.github.telegabots.handler.InvalidCommandWithoutStringParam.execute(int)", ex3.message)
    }

    @Test
    fun testValidate_Fail_WithCommandContextInParam() {
        val ex = assertThrows<IllegalStateException> { commandValidator.validate(CommandWithCommandContextParam::class.java) }

        assertEquals("CommandContext can not be used as handler parameter. Use \"context\" field instead. Handler: public final void org.github.telegabots.handler.CommandWithCommandContextParam.handle(java.lang.String,org.github.telegabots.api.CommandContext)", ex.message)
    }
}

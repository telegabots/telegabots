package org.github.telegabots.util

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.MessageType
import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler
import java.lang.reflect.Method

object CommandClassUtil {
    fun getHandlers(command: BaseCommand): List<CommandHandlerInfo> {
        return command.javaClass.methods
            .mapNotNull { mapHandler(it, command) }
            .map { checkHandler(it) }
    }

    private fun checkHandler(handler: CommandHandlerInfo): CommandHandlerInfo {
        when (handler.messageType) {
            MessageType.Text -> checkTextHandler(handler)
            MessageType.Inline -> checkInlineHandler(handler)
        }

        return handler
    }

    private fun checkInlineHandler(handler: CommandHandlerInfo) {
        check(handler.params.isNotEmpty()) { "Handler must contains at least one parameter: ${handler.method}" }

        val firstParam = handler.params[0]

        check(firstParam.isString()) { "First parameter must be String but found ${firstParam.type.name} in handler ${handler.method}" }
        check(handler.isVoidReturnType()) { "Handler must return void type but it returns ${handler.retType} in method ${handler.method}" }
        check(handler.params.none { it.isContext() }) { "CommandContext can not be used as handler parameter. Use \"context\" field instead. Handler: ${handler.method}" }
    }

    private fun checkTextHandler(handler: CommandHandlerInfo) {
        check(handler.params.isNotEmpty()) { "Handler must contains at least one parameter: ${handler.method}" }

        val firstParam = handler.params[0]

        check(firstParam.isString()) { "First parameter must be String but found ${firstParam.type.name} in handler ${handler.method}" }
        check(handler.isValidReturnType()) { "Handler must return bool or void but it returns ${handler.retType} in method ${handler.method}" }
        check(handler.params.none { it.isContext() }) { "CommandContext can not be used as handler parameter. Use \"context\" field instead. Handler: ${handler.method}" }
    }

    private fun mapHandler(method: Method, command: BaseCommand): CommandHandlerInfo? {
        val messageType = getMessageType(method)

        return if (messageType != null) CommandHandlerInfo(
            name = method.name, method = method,
            params = HandlerParamUtil.getParams(method),
            messageType = messageType,
            retType = method.returnType,
            command = command
        )
        else
            null
    }

    private fun getMessageType(method: Method): MessageType? {
        return if (method.annotations.any { it.annotationClass == TextHandler::class }) {
            MessageType.Text
        } else if (method.annotations.any { it.annotationClass == InlineHandler::class }) {
            MessageType.Inline
        } else {
            null
        }
    }
}

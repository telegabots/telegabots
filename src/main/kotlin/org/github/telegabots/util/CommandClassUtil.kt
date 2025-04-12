package org.github.telegabots.util

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.EmptyCommand
import org.github.telegabots.api.MessageType
import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler
import java.lang.reflect.Method

/**
 * Utility class to work with [CommandHandlerInfo]
 */
internal object CommandClassUtil {
    fun getHandlers(command: BaseCommand): List<CommandHandlerInfo> {
        return command.javaClass.methods
            .mapNotNull { mapHandler(it, command) }
            .map { checkHandler(it) }
    }

    fun checkHandlers(clazz: Class<BaseCommand>) {
        val handlers = clazz.methods
            .mapNotNull { method -> mapHandler(method, EmptyCommand.INSTANCE) }
            .map { method -> checkHandler(method) }

        if (handlers.isEmpty()) {
            error("Command class must contain at least one handler: ${clazz.name}")
        }

        handlers.groupBy { it.messageType }
            .filter { it.value.size > 1 }
            .forEach { (type, methods) ->
                val methods = methods.joinToString(", ") { it.method.name }
                error("Command class ${clazz.name} must contain only one handler of type $type. Found methods: $methods")
            }
    }

    private fun checkHandler(handler: CommandHandlerInfo): CommandHandlerInfo {
        when (handler.messageType) {
            MessageType.Text -> checkTextHandler(handler)
            MessageType.Inline -> checkInlineHandler(handler)
            MessageType.Photo -> checkPhotoHandler(handler)
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

    private fun checkPhotoHandler(handler: CommandHandlerInfo) {
        checkInlineHandler(handler)
    }

    private fun checkTextHandler(handler: CommandHandlerInfo) {
        check(handler.params.isNotEmpty()) { "Handler must contains at least one parameter: ${handler.method}" }

        val firstParam = handler.params[0]

        check(firstParam.isString()) { "First parameter must be String but found ${firstParam.type.name} in handler ${handler.method}" }
        check(handler.isValidReturnType()) { "Handler must return bool or void but it returns ${handler.retType} in method ${handler.method}" }
        check(handler.params.none { it.isContext() }) { "CommandContext can not be used as handler parameter. Use \"context\" field instead. Handler: ${handler.method}" }
    }

    private fun mapHandler(method: Method, command: BaseCommand): CommandHandlerInfo? =
        getMessageType(method)?.let { messageType ->
            CommandHandlerInfo(
                name = method.name, method = method,
                params = HandlerParamUtil.getParams(method),
                messageType = messageType,
                retType = method.returnType,
                command = command
            )
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

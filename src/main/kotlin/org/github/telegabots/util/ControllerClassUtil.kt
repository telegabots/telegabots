package org.github.telegabots.util

import org.github.telegabots.api.BaseController
import org.github.telegabots.api.EmptyController
import org.github.telegabots.api.HandlerType
import org.github.telegabots.api.annotation.ErrorHandler
import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Utility class to work with [ControllerHandlerInfo]
 */
internal object ControllerClassUtil {
    fun getHandlers(controller: BaseController): List<ControllerHandlerInfo> {
        return getMethods(controller.javaClass)
            .mapNotNull { mapHandler(it, controller) }
            .map { checkHandler(it) }
    }

    fun checkHandlers(clazz: Class<BaseController>) {
        val handlers = getMethods(clazz)
            .mapNotNull { method -> mapHandler(method, EmptyController.INSTANCE) }
            .map { method -> checkHandler(method) }

        if (handlers.isEmpty()) {
            error("Controller class must contain at least one handler: ${clazz.name}")
        }

        handlers.map { it.method }.forEach { method ->
            // Check that handler method is public
            check(Modifier.isPublic(method.modifiers)) {
                "Handler method '${method.name}' in controller ${method.declaringClass.name} must be public"
            }
        }
        // TODO: check that in controller exists only one handler for each Exception type
        handlers.groupBy { it.handlerType }
            .filter { it.value.size > 1 && it.key != HandlerType.Error } // Error handlers can be multiple
            .forEach { (type, methods) ->
                val methods = methods.joinToString(", ") { it.method.name }
                error("Controller class '${clazz.name}' must contain only one handler of type $type. Found methods: $methods")
            }
    }

    private fun getMethods(clazz: Class<BaseController>): List<Method> {
        val declaredMethods = clazz.declaredMethods.toList()
        val superClass = clazz.superclass

        return declaredMethods + if (BaseController::class.java.isAssignableFrom(clazz.superclass)) {
            getMethods(superClass as Class<BaseController>)
        } else {
            emptyList()
        }
    }

    private fun checkHandler(handler: ControllerHandlerInfo): ControllerHandlerInfo {
        when (handler.handlerType) {
            HandlerType.Text -> checkTextHandler(handler)
            HandlerType.Inline -> checkInlineHandler(handler)
            HandlerType.Error -> checkErrorHandler(handler)
        }

        return handler
    }

    private fun checkInlineHandler(handler: ControllerHandlerInfo) {
        check(handler.params.isNotEmpty()) { "Handler must contains at least one parameter: ${handler.method}" }

        val firstParam = handler.params[0]

        check(firstParam.isString()) { "First parameter must be String but found ${firstParam.type.name} in handler ${handler.method}" }
        check(handler.isVoidReturnType()) { "Handler must return void type but it returns ${handler.retType} in method ${handler.method}" }
        check(handler.params.none { it.isContext() }) { "ControllerContext can not be used as handler parameter. Use \"context\" field instead. Handler: ${handler.method}" }
    }

    private fun checkTextHandler(handler: ControllerHandlerInfo) {
        check(handler.params.isNotEmpty()) { "Handler must contains at least one parameter: ${handler.method}" }

        val firstParam = handler.params[0]

        check(firstParam.isString()) { "First parameter must be String but found ${firstParam.type.name} in handler ${handler.method}" }
        check(handler.isValidReturnType()) { "Handler must return bool or void but it returns ${handler.retType} in method ${handler.method}" }
        check(handler.params.none { it.isContext() }) { "ControllerContext can not be used as handler parameter. Use \"context\" field instead. Handler: ${handler.method}" }
    }

    private fun checkErrorHandler(handler: ControllerHandlerInfo) {
        check(handler.params.isNotEmpty()) { "Handler must contains at least one Throwable parameter: ${handler.method}" }

        val firstParam = handler.params[0]

        check(firstParam.isException()) { "First parameter must be Throwable but found ${firstParam.type.name} in handler ${handler.method}" }
        check(handler.isVoidReturnType()) { "Handler must return void type but it returns ${handler.retType} in method ${handler.method}" }
        check(handler.params.none { it.isContext() }) { "ControllerContext can not be used as handler parameter. Use \"context\" field instead. Handler: ${handler.method}" }

        val secondParam = if (handler.params.size > 1) handler.params[1] else null
        if (secondParam != null) {
            check(secondParam.isString()) { "Second parameter must be String but found ${secondParam.type.name} in handler ${handler.method}" }
        }
    }

    private fun mapHandler(method: Method, controller: BaseController): ControllerHandlerInfo? =
        getMessageType(method)?.let { handlerType ->
            ControllerHandlerInfo(
                name = method.name, method = method,
                params = HandlerParamUtil.getParams(method),
                handlerType = handlerType,
                retType = method.returnType,
                controller = controller
            )
        }

    private fun getMessageType(method: Method): HandlerType? {
        return if (method.annotations.any { it.annotationClass == TextHandler::class }) {
            HandlerType.Text
        } else if (method.annotations.any { it.annotationClass == InlineHandler::class }) {
            HandlerType.Inline
        } else if (method.annotations.any { it.annotationClass == ErrorHandler::class }) {
            HandlerType.Error
        } else {
            null
        }
    }
}

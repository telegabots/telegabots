package org.github.telegabots.service

import org.github.telegabots.api.BaseController
import org.github.telegabots.api.ControllerContext
import org.github.telegabots.api.HandlerType
import org.github.telegabots.api.MessageType
import org.github.telegabots.context.ControllerContextSupport
import org.github.telegabots.error.ControllerInvokeException
import org.github.telegabots.state.States
import org.github.telegabots.util.ControllerHandlerInfo
import org.slf4j.LoggerFactory
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Keep stateless [BaseController] and its [ControllerHandlerInfo]s.
 */
internal class ControllerHandler(
    val controller: BaseController,
    private val handlers: List<ControllerHandlerInfo>
) {
    val controllerClass: Class<out BaseController> get() = controller.javaClass
    private val textHandler: ControllerHandlerInfo? = handlers.find { p -> p.handlerType == HandlerType.Text }
    private val inlineHandler: ControllerHandlerInfo? = handlers.find { p -> p.handlerType == HandlerType.Inline }

    fun executeText(text: String, states: States, context: ControllerContext): Boolean {
        checkNotNull(textHandler) { "Text message handler not implemented in ${controller.javaClass.name}. Annotate method with @TextHandler" }

        try {
            setContext(context)

            return textHandler.executeText(text, states, context)
        } catch (ex: ControllerInvokeException) {
            log.error(
                "Exception in text handler: ${controller.javaClass.name}.${textHandler.method.name}(), handling message \"{}\"",
                text,
                ex
            )
            val errorHandler = getErrorHandler(ex)
            if (errorHandler != null) {
                errorHandler.handleError(ex, states, context, text)
                return false
            }
            throw ex
        } finally {
            clearContext()
        }
    }

    fun executeInline(data: String, states: States, context: ControllerContext) {
        checkNotNull(inlineHandler) { "Inline message handler not implemented in ${controller.javaClass.name}. Annotate method with @InlineHandler" }

        try {
            setContext(context)
            inlineHandler.executeInline(data, states, context)
        } catch (ex: ControllerInvokeException) {
            log.error(
                "Exception in inline handler: ${controller.javaClass.name}.${inlineHandler.method.name}(), handling message \"{}\"",
                data,
                ex
            )
            val errorHandler = getErrorHandler(ex)
            if (errorHandler != null) {
                errorHandler.handleError(ex, states, context, data)
                return
            }
            throw ex
        } finally {
            clearContext()
        }
    }

    /**
     * Returns error handler for given exception if it exists.
     */
    private fun getErrorHandler(ex: ControllerInvokeException): ControllerHandlerInfo? {
        val cause = ex.cause ?: return null
        val errorHandlers = handlers.filter { it.handlerType == HandlerType.Error }

        // Check if the first parameter of the handler matches the cause type exactly
        var errorType: Class<*> = cause.javaClass
        while (errorType != Object::class.java) {
            val errorHandler = errorHandlers.find { handler ->
                handler.params.isNotEmpty() && handler.params[0].type == errorType
            }
            if (errorHandler != null) {
                return errorHandler
            }
            // If not found, try to find a handler with a superclass type
            errorType = errorType.superclass
        }
        return errorHandlers.find { handler ->
            // try to find a handler with parameter type that is assignable from a cause type
            handler.params.isNotEmpty() && handler.params[0].type.isAssignableFrom(cause.javaClass)
        }
    }

    fun canHandle(messageType: MessageType): Boolean =
        when (messageType) {
            MessageType.Text -> textHandler != null
            MessageType.Inline, MessageType.Photo -> inlineHandler != null
        }

    override fun toString(): String {
        return "ControllerHandler(controller=$controller)"
    }

    /**
     * Sets current context of controller
     */
    private fun setContext(context: ControllerContext?) {
        val prop = BaseController::class.memberProperties.find { it.name == "context" }
            ?: throw IllegalStateException("Context not found in controller: ${controller.javaClass.name}")
        prop.isAccessible = true
        (prop.get(controller) as ControllerContextSupport).setContext(context)
    }

    private fun clearContext() {
        setContext(null)
    }

    private companion object {
        val log = LoggerFactory.getLogger(ControllerHandler::class.java)!!
    }
}

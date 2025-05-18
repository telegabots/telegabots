package org.github.telegabots.service

import org.github.telegabots.api.BaseController
import org.github.telegabots.api.ControllerContext
import org.github.telegabots.api.MessageType
import org.github.telegabots.context.ControllerContextSupport
import org.github.telegabots.state.States
import org.github.telegabots.util.ControllerHandlerInfo
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
    private val textHandler: ControllerHandlerInfo? = handlers.find { p -> p.messageType == MessageType.Text }
    private val inlineHandler: ControllerHandlerInfo? = handlers.find { p -> p.messageType == MessageType.Inline }

    fun executeText(text: String, states: States, context: ControllerContext): Boolean {
        checkNotNull(textHandler) { "Text message handler not implemented in ${controller.javaClass.name}. Annotate method with @TextHandler" }

        try {
            setContext(context)

            return textHandler.executeText(text, states, context)
        } finally {
            clearContext()
        }
    }

    fun executeInline(data: String, states: States, context: ControllerContext) {
        checkNotNull(inlineHandler) { "Inline message handler not implemented in ${controller.javaClass.name}. Annotate method with @InlineHandler" }

        try {
            setContext(context)
            inlineHandler.executeInline(data, states, context)
        } finally {
            clearContext()
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
}

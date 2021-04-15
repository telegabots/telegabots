package org.github.telegabots.service

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.CommandContext
import org.github.telegabots.api.CommandInterceptor
import org.github.telegabots.api.MessageType
import org.github.telegabots.context.CommandContextSupport
import org.github.telegabots.state.States
import org.github.telegabots.util.CommandHandlerInfo
import org.slf4j.LoggerFactory
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Keep stateless command and meta-info about one
 */
class CommandHandler(
    val command: BaseCommand,
    private val handlers: List<CommandHandlerInfo>,
    private val commandInterceptor: CommandInterceptor
) {
    val commandClass: Class<out BaseCommand> get() = command.javaClass
    private val textHandler = handlers.find { p -> p.messageType == MessageType.Text }
    private val inlineHandler = handlers.find { p -> p.messageType == MessageType.Inline }

    fun executeText(text: String, states: States, context: CommandContext): Boolean {
        checkNotNull(textHandler) { "Text message handler not implemented in ${command.javaClass.name}. Annotate method with @TextHandler" }

        try {
            setContext(context)
            val result = textHandler.executeText(text, states, context)

            try {
                commandInterceptor.executed(command, MessageType.Text)
            } catch (ex: Exception) {
                log.error(
                    "Interceptor call failed on command {} with error: {}",
                    command.javaClass.simpleName,
                    ex.message,
                    ex
                )
            }

            return result
        } finally {
            clearContext()
        }
    }

    fun executeInline(data: String, states: States, context: CommandContext) {
        checkNotNull(inlineHandler) { "Inline message handler not implemented in ${command.javaClass.name}. Annotate method with @InlineHandler" }

        try {
            setContext(context)
            inlineHandler.executeInline(data, states, context)

            try {
                commandInterceptor.executed(command, MessageType.Inline)
            } catch (ex: Exception) {
                log.error(
                    "Interceptor call failed on command {} with error: {}",
                    command.javaClass.simpleName,
                    ex.message,
                    ex
                )
            }
        } finally {
            clearContext()
        }
    }

    fun canHandle(messageType: MessageType): Boolean =
        when (messageType) {
            MessageType.Text -> textHandler != null
            MessageType.Inline -> inlineHandler != null
        }

    override fun toString(): String {
        return "CommandHandler(command=$command)"
    }

    /**
     * Sets current context of command
     */
    private fun setContext(context: CommandContext?) {
        val prop = BaseCommand::class.memberProperties.find { it.name == "context" }
            ?: throw IllegalStateException("Context not found in command: ${command.javaClass.name}")
        prop.isAccessible = true
        (prop.get(command) as CommandContextSupport).setContext(context)
    }

    private fun clearContext() {
        setContext(null)
    }

    companion object {
        private val log = LoggerFactory.getLogger(CommandHandler::class.java)!!
    }
}

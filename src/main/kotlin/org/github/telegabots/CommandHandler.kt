package org.github.telegabots

import org.github.telegabots.context.CommandContextSupport
import org.github.telegabots.state.States
import org.github.telegabots.util.HandlerInfo
import org.slf4j.LoggerFactory
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Keep stateless command and meta-info about one
 */
class CommandHandler(
    val command: BaseCommand,
    private val handlers: List<HandlerInfo>,
    private val commandInterceptor: CommandInterceptor
) {
    private val executeHandler = handlers.find { p -> p.messageType == MessageType.Text }
    private val executeCallbackHandler = handlers.find { p -> p.messageType == MessageType.Callback }

    fun execute(text: String, states: States, context: CommandContext): Boolean {
        checkNotNull(executeHandler) { "Method execute not implemented for ${command.javaClass.name}" }

        try {
            setContext(context)
            val result = executeHandler.execute(text, states, context)

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

    fun executeCallback(messageId: Int, data: String, states: States, context: CommandContext) {
        checkNotNull(executeCallbackHandler) { "Method executeCallback not implemented for ${command.javaClass.name}" }

        try {
            setContext(context)
            executeCallbackHandler.executeCallback(messageId, data, states, context)

            try {
                commandInterceptor.executed(command, MessageType.Callback)
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
            MessageType.Text -> executeHandler != null
            MessageType.Callback -> executeCallbackHandler != null
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

package org.github.telegabots

import org.github.telegabots.state.States
import org.github.telegabots.util.HandlerInfo
import org.slf4j.LoggerFactory

/**
 * Keep stateless command and meta-info about one
 */
class CommandHandler(
    val command: BaseCommand,
    private val handlers: List<HandlerInfo>,
    private val commandInterceptor: CommandInterceptor
) {
    private val executeHandler = handlers.find { p -> p.messageType == MessageType.TEXT }
    private val executeCallbackHandler = handlers.find { p -> p.messageType == MessageType.CALLBACK }

    fun execute(text: String, states: States, context: CommandContext): Boolean {
        checkNotNull(executeHandler) { "Method execute not implemented for ${command.javaClass.name}" }

        val result = executeHandler.execute(text, states, context)

        try {
            commandInterceptor.executed(command, MessageType.TEXT)
        } catch (ex: Exception) {
            log.error("Interceptor call failed on command {} with error: {}", command.javaClass.simpleName, ex.message, ex)
        }

        return result
    }

    fun executeCallback(messageId: Int, data: String, states: States, context: CommandContext) {
        checkNotNull(executeCallbackHandler) { "Method executeCallback not implemented for ${command.javaClass.name}" }

        executeCallbackHandler.executeCallback(messageId, data, states, context)

        try {
            commandInterceptor.executed(command, MessageType.CALLBACK)
        } catch (ex: Exception) {
            log.error("Interceptor call failed on command {} with error: {}", command.javaClass.simpleName, ex.message, ex)
        }
    }

    fun canHandle(messageType: MessageType): Boolean =
        when (messageType) {
            MessageType.TEXT -> executeHandler != null
            MessageType.CALLBACK -> executeCallbackHandler != null
        }

    override fun toString(): String {
        return "CommandHandler(command=$command)"
    }

    companion object {
        private val log = LoggerFactory.getLogger(CommandHandler::class.java)!!
    }
}

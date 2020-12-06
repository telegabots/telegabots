package org.github.telegabots

import org.github.telegabots.state.States
import org.github.telegabots.util.HandlerInfo

/**
 * Keep stateless command and meta-info about one
 */
class CommandHandler(val command: BaseCommand,
                     private val handlers: List<HandlerInfo>) {
    private val executeHandler = handlers.find { p -> p.messageType == MessageType.TEXT }
    private val executeCallbackHandler = handlers.find { p -> p.messageType == MessageType.CALLBACK }

    fun execute(text: String, states: States, context: CommandContext): Boolean {
        return executeHandler?.execute(text, states, context)
                ?: throw IllegalStateException("Method execute not implemented for ${command.javaClass.name}")
    }

    fun executeCallback(messageId: Int, data: String, states: States, context: CommandContext) {
        executeCallbackHandler?.executeCallback(messageId, data, states, context)
                ?: throw IllegalStateException("Method executeCallback not implemented for ${command.javaClass.name}")
    }

    fun canHandle(messageType: MessageType): Boolean =
            when (messageType) {
                MessageType.TEXT -> executeHandler != null
                MessageType.CALLBACK -> executeCallbackHandler != null
            }

    override fun toString(): String {
        return "CommandHandler(command=$command)"
    }
}

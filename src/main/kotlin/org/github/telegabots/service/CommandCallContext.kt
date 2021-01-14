package org.github.telegabots.service

import org.github.telegabots.api.CommandContext
import org.github.telegabots.api.InputMessage
import org.github.telegabots.api.MessageType
import org.github.telegabots.state.States
import org.slf4j.LoggerFactory

/**
 * Composing command handler, input message and state
 */
class CommandCallContext(
    private val commandHandler: CommandHandler,
    private val input: InputMessage,
    private val states: States,
    private val commandContext: CommandContext,
    private val defaultContext: () -> CommandCallContext?
) {
    private val log = LoggerFactory.getLogger(CommandCallContext::class.java)

    fun execute(): Boolean {
        if (!commandHandler.canHandle(input.type)) {
            throw IllegalStateException("Message of type ${input.type} can not be handled by command: ${commandHandler.command.javaClass.name}")
        }

        val success = when (input.type) {
            MessageType.Text -> commandHandler.executeText(input.query, states, commandContext)
            MessageType.Inline -> {
                commandHandler.executeInline(input.query, states, commandContext)
                true
            }
        }

        log.debug("Flush states for handler: {}, input: {}", commandHandler, input)
        states.flush()

        if (!success) {
            val defaultContext = defaultContext()

            if (defaultContext != null) {
                log.warn(
                    "Call default context ({}). Because command '{}' cannot handle input message: {}",
                    defaultContext, commandHandler, input
                )
                return defaultContext.execute()
            }
        }

        return success
    }

    override fun toString(): String {
        return "CommandCallContext(commandHandler=$commandHandler, input=$input)"
    }
}

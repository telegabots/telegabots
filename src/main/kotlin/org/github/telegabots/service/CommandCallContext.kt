package org.github.telegabots.service

import org.github.telegabots.api.CommandContext
import org.github.telegabots.api.InputMessage
import org.github.telegabots.api.MessageType
import org.github.telegabots.state.StateKind
import org.github.telegabots.state.States
import org.slf4j.LoggerFactory


interface CommandCallContext {
    fun execute(): Boolean
}

/**
 * Composing command handler, input message and state
 */
class CommandCallContextImpl(
    private val commandHandler: CommandHandler,
    private val states: States,
    private val commandContext: CommandContext,
    private val defaultContext: () -> CommandCallContext?
) : CommandCallContext {
    private val log = LoggerFactory.getLogger(CommandCallContext::class.java)
    private val input: InputMessage = commandContext.inputMessage()

    override fun execute(): Boolean {
        if (!commandHandler.canHandle(input.type)) {
            if (input.type == MessageType.Text) {
                val defaultContext = defaultContext()

                if (defaultContext != null) {
                    log.warn(
                        "Call default context ({}). Because command '{}' cannot handle input message: {}",
                        defaultContext, commandHandler.command.javaClass.name, input
                    )

                    return defaultContext.execute()
                }
            }

            throw IllegalStateException("Message of type ${input.type} can not be handled by command: ${commandHandler.command.javaClass.name}")
        }

        if (log.isTraceEnabled) {
            log.trace(
                """
                ----------------------------------------------------------
                Command: [{}]:{}
                Handler: {}
                Block/Page: {}/{}
                State:
                  local: {}
                  shared: {}
                  user: {}
                  global: {}
                ----------------------------------------------------------
            """.trimIndent(), input.type, input.query, commandHandler.command,
                commandContext.blockId(),
                commandContext.pageId(),
                states.getAll(StateKind.LOCAL),
                states.getAll(StateKind.SHARED),
                states.getAll(StateKind.USER),
                states.getAll(StateKind.GLOBAL)
            )
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
                    defaultContext, commandHandler.command.javaClass.name, input
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

object NothingCommandCallContext : CommandCallContext {
    override fun execute(): Boolean = true
}

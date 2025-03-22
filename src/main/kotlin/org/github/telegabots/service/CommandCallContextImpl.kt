package org.github.telegabots.service

import org.github.telegabots.api.CommandContext
import org.github.telegabots.api.CommandInterceptor
import org.github.telegabots.api.InputMessage
import org.github.telegabots.api.MessageType
import org.github.telegabots.state.StateKind
import org.github.telegabots.state.States
import org.slf4j.LoggerFactory

/**
 * Composing command handler, input message and state
 */
internal class CommandCallContextImpl(
    private val commandHandler: CommandHandler,
    private val states: States,
    private val commandContext: CommandContext,
    private val defaultContext: () -> CommandCallContext?
) : CommandCallContext {
    private val log = LoggerFactory.getLogger(CommandCallContext::class.java)!!
    private val input: InputMessage = commandContext.inputMessage()

    override fun execute(): Boolean {
        if (!commandHandler.canHandle(input.type)) {
            if (input.type == MessageType.Text) {
                val success = callDefaultContext()
                if (success != null) {
                    return success
                }
            }

            throw IllegalStateException("Message of type ${input.type} can not be handled by command: ${commandHandler.command.javaClass.name}")
        }

        logContext()

        val success = when (input.type) {
            MessageType.Text -> commandHandler.executeText(input.query, states, commandContext)
            MessageType.Inline -> {
                commandHandler.executeInline(input.query, states, commandContext)
                true
            }
        }

        commandContext.getService(CommandInterceptor::class.java)?.let { commandInterceptor ->
            try {
                commandInterceptor.executed(commandHandler.command, input.type, success)
            } catch (ex: Exception) {
                log.error(
                    "Interceptor call failed on command {} with error: {}",
                    commandHandler.command.javaClass.simpleName,
                    ex.message,
                    ex
                )
            }
        }

        states.flush()

        if (!success) {
            return callDefaultContext() ?: false
        }

        return true
    }

    private fun callDefaultContext(): Boolean? {
        val defaultContext = defaultContext()

        if (defaultContext != null) {
            log.warn(
                "Call default context ({}). Because command '{}' cannot handle input message: {}",
                defaultContext, commandHandler.command.javaClass.name, input
            )
            return defaultContext.execute()
        }

        return null
    }

    private fun logContext() {
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
    }

    override fun toString(): String {
        return "CommandCallContext(commandHandler=$commandHandler, input=$input)"
    }
}

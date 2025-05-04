package org.github.telegabots.api

/**
 * Context used by any implementation of [BaseCommand]
 */
interface CommandContext : BaseContext, UserContext {
    /**
     * Message from user
     */
    fun inputMessage(): InputMessage

    /**
     * Message id related with input user message
     */
    fun inputMessageId(): Int

    /**
     * Returns current command in which input handled
     */
    fun currentCommand(): BaseCommand

    /**
     * Execute command within current context
     */
    fun execute(): Boolean
}

package org.github.telegabots.api

/**
 * Context used by any implementation of [BaseController]
 */
interface ControllerContext : BaseContext, UserContext {
    /**
     * Message from user
     */
    fun inputMessage(): InputMessage

    /**
     * Message id related with an input user message
     */
    fun inputMessageId(): Int

    /**
     * Returns current controller in which input handled
     */
    fun currentController(): BaseController

    /**
     * Execute controller within the current context
     */
    fun execute(): Boolean
}

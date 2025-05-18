package org.github.telegabots.api

/**
 * For internal uses. Used in tests.
 */
internal interface ControllerInterceptor : Service {
    /**
     * Called when controller executed
     *
     * @param controller Controller instance
     * @param messageType Message type
     * @param result Execution result
     */
    fun executed(controller: BaseController, messageType: MessageType, result: Boolean)
}

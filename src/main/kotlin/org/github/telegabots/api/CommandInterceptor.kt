package org.github.telegabots.api

/**
 * For internal uses. Used in tests.
 */
internal interface CommandInterceptor : Service {
    /**
     * Called when command executed.
     *
     * @param command Command instance
     * @param messageType Message type
     * @param result Execution result
     */
    fun executed(command: BaseCommand, messageType: MessageType, result: Boolean)
}

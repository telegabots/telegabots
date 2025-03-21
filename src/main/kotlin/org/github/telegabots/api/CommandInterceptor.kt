package org.github.telegabots.api

/**
 * For internal uses
 */
interface CommandInterceptor {
    fun executed(command: BaseCommand, messageType: MessageType)

    companion object {
        @JvmStatic
        val Empty: CommandInterceptor = CommandInterceptorEmpty
    }
}

internal object CommandInterceptorEmpty : CommandInterceptor {
    override fun executed(command: BaseCommand, messageType: MessageType) {
    }
}

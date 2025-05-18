package org.github.telegabots.api

import org.github.telegabots.api.config.BotConfig
import java.util.function.Consumer

/**
 * Builder for [TelegaBotStarter]
 */
class TelegaBotBuilder(private val rootController: Class<out BaseController>) {
    private var config: BotConfig? = null
    private var serviceProvider: ServiceProvider = EmptyServiceProvider()
    private var messageSender: MessageSender? = null
    private var onStartHandler: Consumer<TelegaBot> = Consumer {}

    fun config(config: BotConfig): TelegaBotBuilder {
        this.config = config
        return this
    }

    fun serviceProvider(serviceProvider: ServiceProvider):TelegaBotBuilder {
        this.serviceProvider = serviceProvider
        return this
    }

    /**
     * Message sender for bot. By default, internal message sender is enough.
     */
    fun messageSender(messageSender: MessageSender):TelegaBotBuilder {
        this.messageSender = messageSender
        return this
    }

    /**
     * Handler for success start
     */
    fun onStartHandler(onStartHandler: Consumer<TelegaBot>): TelegaBotBuilder {
        this.onStartHandler = onStartHandler
        return this
    }

    /**
     * Creates [TelegaBotStarter] instance
     */
    fun build(): TelegaBotStarter {
        checkNotNull(config) { "config can not be null" }

        return TelegaBotStarter(
            config = config!!,
            serviceProvider = serviceProvider,
            rootController = rootController,
            messageSender = messageSender,
            onStartHandler = onStartHandler
        )
    }
}

private class EmptyServiceProvider : ServiceProvider

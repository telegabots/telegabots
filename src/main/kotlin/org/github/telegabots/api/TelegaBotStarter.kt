package org.github.telegabots.api

import org.github.telegabots.api.config.BotConfig
import org.github.telegabots.service.MessageSenderImpl
import org.github.telegabots.state.MemoryStateDbProvider
import org.github.telegabots.state.StateDbProvider
import org.github.telegabots.state.sqlite.SqliteStateDbProvider
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.util.function.Consumer

/**
 * Entry-point of Telegram bot
 */
open class TelegaBotStarter(
    private val config: BotConfig,
    private val serviceProvider: ServiceProvider,
    private val rootCommand: Class<out BaseCommand>,
    private val stateDbProvider: StateDbProvider = createStateProvider(config),
    private val messageSender: MessageSender? = null
) : TelegramLongPollingBot() {
    protected val log = LoggerFactory.getLogger(javaClass)!!
    protected val messageSenderReal =
        messageSender ?: MessageSenderImpl(this, ignoreNotModifiedMessageError = config.notModifiedMessageErrorIgnore)
    protected val telegaBot: TelegaBot = TelegaBot(
        messageSender = messageSenderReal,
        serviceProvider = serviceProvider,
        config = config,
        rootCommand = rootCommand,
        dbProvider = stateDbProvider
    )

    override fun getBotToken(): String = config.botToken

    override fun getBotUsername(): String = config.botName

    override fun onUpdateReceived(update: Update) {
        telegaBot.handle(update)
    }

    /**
     * Starts bot with rootCommand as entry point
     */
    fun start(onSuccessHandler: Consumer<MessageSender> = Consumer { }) {
        val telegramBotsApi = TelegramBotsApi()

        try {
            telegramBotsApi.registerBot(this)
            onSuccessHandler.accept(messageSenderReal)
        } catch (e: TelegramApiException) {
            log.error("Bot register failed: {}", e.message)
            throw e
        }
    }

    fun <T : Service> getService(clazz: Class<T>): T? = telegaBot.getService(clazz)

    private companion object {
        init {
            ApiContextInitializer.init()
        }

        fun createStateProvider(config: BotConfig): StateDbProvider =
            if (config.stateDbPath.isNotBlank())
                SqliteStateDbProvider.create(config.stateDbPath)
            else
                MemoryStateDbProvider()
    }
}

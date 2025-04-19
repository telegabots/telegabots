package org.github.telegabots.api

import org.github.telegabots.api.config.BotConfig
import org.github.telegabots.service.MessageSenderImpl
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import java.util.function.Consumer

/**
 * Entry-point of Telegram bot
 */
open class TelegaBotStarter(
    private val config: BotConfig,
    serviceProvider: ServiceProvider,
    rootCommand: Class<out BaseCommand>,
    messageSender: MessageSender? = null,
    private val onStartHandler: Consumer<TelegaBot> = Consumer { }
) : TelegramLongPollingBot(config.botToken) {
    private val messageSenderReal = messageSender
        ?: MessageSenderImpl(this, ignoreNotModifiedMessageError = config.notModifiedMessageErrorIgnore)
    private val telegaBot: TelegaBot = TelegaBot(
        messageSender = messageSenderReal,
        userServiceProvider = serviceProvider,
        config = config,
        rootCommand = rootCommand
    )

    override fun getBotUsername(): String = config.botName

    override fun onUpdateReceived(update: Update) {
        telegaBot.handle(update)
    }

    /**
     * Starts bot with rootCommand as entry point
     */
    fun start() {
        val telegramBotsApi = TelegramBotsApi(DefaultBotSession::class.java)

        try {
            telegramBotsApi.registerBot(this)
            onStartHandler.accept(telegaBot)
        } catch (ex: TelegramApiException) {
            log.error("Bot register failed: {}", ex.message, ex)
            throw ex
        }
    }

    fun <T : Service> getService(clazz: Class<T>): T = telegaBot.getService(clazz)

    fun <T : Service> tryGetService(clazz: Class<T>): T? = telegaBot.tryGetService(clazz)

    companion object {
        private val log = LoggerFactory.getLogger(TelegaBotStarter::class.java)!!

        @JvmStatic
        fun builder(rootCommand: Class<out BaseCommand>): TelegaBotBuilder {
            return TelegaBotBuilder(rootCommand)
        }
    }
}

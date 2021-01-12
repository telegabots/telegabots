package org.examples.allfeaturedbot

import org.examples.allfeaturedbot.commands.RootCommand
import org.github.telegabots.api.*
import org.github.telegabots.api.config.BotConfig
import org.github.telegabots.state.MemoryStateDbProvider
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.io.Serializable
import java.util.function.Consumer

class AllFeaturedBot(private val config: BotConfig) : TelegramLongPollingBot(), MessageSender, ServiceProvider {
    private val log = LoggerFactory.getLogger(javaClass)!!
    private val telegaBot = TelegaBot(
        messageSender = this,
        serviceProvider = this,
        adminChatId = config.adminChatId,
        rootCommand = RootCommand::class.java,
        dbProvider = MemoryStateDbProvider()
    )

    init {
        sendMessage(config.adminChatId.toString(), "*All featured bot started*", ContentType.Markdown)
    }

    override fun getBotToken(): String = config.botToken

    override fun getBotUsername(): String = config.botName

    override fun onUpdateReceived(update: Update) {
        telegaBot.handle(update)
    }

    override fun sendMessage(
        chatId: String,
        message: String,
        contentType: ContentType,
        disablePreview: Boolean,
        preSendHandler: Consumer<SendMessage>
    ): Int {
        val sendMessage = SendMessage()
        sendMessage.chatId = chatId
        sendMessage.text = message

        if (contentType == ContentType.Html) {
            sendMessage.enableHtml(true)
        } else if (contentType == ContentType.Markdown) {
            sendMessage.enableMarkdown(true)
        }

        if (disablePreview) {
            sendMessage.disableWebPagePreview()
        }

        preSendHandler.accept(sendMessage)

        log.info("send (length: {}): {}", message.length, message)

        try {
            val resp = execute<Message, SendMessage>(sendMessage)
            return resp.messageId
        } catch (e: Exception) {
            log.error("send message failed: {}, message: {}", e.message, sendMessage)
            throw e
        }
    }

    override fun updateMessage(
        chatId: String,
        messageId: Int,
        message: String,
        contentType: ContentType,
        disablePreview: Boolean,
        preSendHandler: Consumer<EditMessageText>
    ) {
        val editMessageText = EditMessageText()
        editMessageText.chatId = chatId
        editMessageText.text = message
        editMessageText.messageId = messageId

        if (contentType == ContentType.Html) {
            editMessageText.enableHtml(true)
        } else if (contentType == ContentType.Markdown) {
            editMessageText.enableMarkdown(true)
        }

        if (disablePreview) {
            editMessageText.disableWebPagePreview()
        }

        preSendHandler.accept(editMessageText)

        log.debug("update message, messageId: {}, (length: {}): {}", messageId, message.length, message)

        try {
            execute<Serializable, EditMessageText>(editMessageText)
        } catch (e: Exception) {
            log.error("edit message failed: {}, message: {}", e.message, editMessageText)
            throw e
        }
    }

    override fun <T : Service> getService(clazz: Class<T>): T? = null

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            ApiContextInitializer.init()
            val telegramBotsApi = TelegramBotsApi()

            try {
                val config = BotConfig.load("application.properties")
                telegramBotsApi.registerBot(AllFeaturedBot(config))
            } catch (e: TelegramApiException) {
                e.printStackTrace()
            }
        }
    }
}

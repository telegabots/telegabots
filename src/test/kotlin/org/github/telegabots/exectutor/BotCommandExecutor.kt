package org.github.telegabots.exectutor

import org.github.telegabots.*
import org.github.telegabots.service.JsonService
import org.github.telegabots.state.MemoryStateDbProvider
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

class BotCommandExecutor(private val rootCommand: Class<out BaseCommand>) : MessageSender {
    private val log = LoggerFactory.getLogger(BotCommandExecutor::class.java)!!
    private val messageIdCounter = AtomicInteger(100_000)
    private val serviceProvider = mock(ServiceProvider::class.java)
    private val dbProvider = MemoryStateDbProvider()
    private val jsonService = JsonService()
    private val telegaBot = TelegaBot(
        messageSender = this,
        serviceProvider = serviceProvider,
        adminChatId = 0,
        dbProvider = dbProvider,
        jsonService = jsonService,
        rootCommand = rootCommand
    )

    fun handle(update: Update): Boolean {
        return telegaBot.handle(update)
    }

    fun addService(service: Class<out Service>, instance: Service) {
        Mockito.`when`(serviceProvider.getService(service)).thenReturn(instance)
    }

    override fun sendMessage(
        chatId: String,
        message: String,
        contentType: ContentType,
        disablePreview: Boolean,
        preSendHandler: Consumer<SendMessage>
    ): Int  {
        // TODO: check method was called
        log.info("sendMessage: chatId: $chatId, message: $message")
        return messageIdCounter.incrementAndGet()
    }

    override fun updateMessage(
        chatId: String,
        messageId: Int,
        message: String,
        contentType: ContentType,
        disablePreview: Boolean,
        preSendHandler: Consumer<EditMessageText>
    ) {
        // TODO: check method was called
        log.info("updateMessage: chatId: $chatId, messageId: $messageId, message: $message")
    }
}

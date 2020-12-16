package org.github.telegabots.exectutor

import org.github.telegabots.*
import org.github.telegabots.service.JsonService
import org.github.telegabots.service.UserLocalizationFactory
import org.github.telegabots.state.MemoryStateDbProvider
import org.github.telegabots.test.TestUserLocalizationProvider
import org.github.telegabots.test.call
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

class BotCommandExecutor(private val rootCommand: Class<out BaseCommand>) : MessageSender, CommandInterceptor {
    private val log = LoggerFactory.getLogger(BotCommandExecutor::class.java)!!
    private val messageIdCounter = AtomicInteger(100_000)
    private val serviceProvider = mock(ServiceProvider::class.java)
    private val dbProvider = MemoryStateDbProvider()
    private val jsonService = JsonService()
    private val userLocalizationFactory = mock(UserLocalizationFactory::class.java)
    private val telegaBot: TelegaBot

    init {
        Mockito.`when`(serviceProvider.tryGetService(UserLocalizationFactory::class.java))
            .thenReturn(userLocalizationFactory)
        Mockito.`when`(serviceProvider.getService(UserLocalizationFactory::class.java))
            .thenReturn(userLocalizationFactory)

        telegaBot = TelegaBot(
            messageSender = this,
            serviceProvider = serviceProvider,
            adminChatId = 0,
            dbProvider = dbProvider,
            jsonService = jsonService,
            rootCommand = rootCommand,
            commandInterceptor = this
        )
    }

    fun handle(update: Update): Boolean {
        return telegaBot.handle(update)
    }

    fun addService(service: Class<out Service>, instance: Service) {
        Mockito.`when`(serviceProvider.getService(service)).thenReturn(instance)
    }

    fun addLocalization(userId: Int, vararg localPairs: Pair<String, String>) {
        Mockito.`when`(userLocalizationFactory.getProvider(userId))
            .thenReturn(TestUserLocalizationProvider(*localPairs))
    }

    override fun sendMessage(
        chatId: String,
        message: String,
        contentType: ContentType,
        disablePreview: Boolean,
        preSendHandler: Consumer<SendMessage>
    ): Int {
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

    override fun executed(command: BaseCommand, messageType: MessageType) {
        command::class.call()
    }
}

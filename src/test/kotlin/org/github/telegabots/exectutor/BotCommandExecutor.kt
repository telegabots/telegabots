package org.github.telegabots.exectutor

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.CommandInterceptor
import org.github.telegabots.api.ContentType
import org.github.telegabots.api.MessageSender
import org.github.telegabots.api.MessageType
import org.github.telegabots.api.Service
import org.github.telegabots.api.ServiceProvider
import org.github.telegabots.api.TelegaBot
import org.github.telegabots.api.UserLocalizationFactory
import org.github.telegabots.api.config.BotConfig
import org.github.telegabots.state.MemoryStateDbProvider
import org.github.telegabots.test.TestUserLocalizationProvider
import org.github.telegabots.test.call
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

class BotCommandExecutor(private val rootCommand: Class<out BaseCommand>) : MessageSender, CommandInterceptor {
    private val log = LoggerFactory.getLogger(BotCommandExecutor::class.java)!!
    private val messageIdCounter = AtomicInteger(100_000)
    private val serviceProvider = mock(ServiceProvider::class.java)
    private val dbProvider = MemoryStateDbProvider()
    private val userLocalizationFactory = mock(UserLocalizationFactory::class.java)
    private val localProviders = mutableMapOf<Long, TestUserLocalizationProvider>()
    private val telegaBot: TelegaBot
    private val sentMessages = mutableMapOf<Int, String>()
    private val config = BotConfig.load(Properties())

    init {
        Mockito.`when`(serviceProvider.getService(UserLocalizationFactory::class.java))
            .thenReturn(userLocalizationFactory)
        Mockito.`when`(userLocalizationFactory.getProvider(anyLong()))
            .thenAnswer { mock -> getLocalizationProvider(mock.arguments[0] as Long) }

        telegaBot = TelegaBot(
            messageSender = this,
            serviceProvider = serviceProvider,
            config = config,
            dbProvider = dbProvider,
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

    fun addLocalization(userId: Long, vararg localPairs: Pair<String, String>) {
        getLocalizationProvider(userId).addLocalization(*localPairs)
    }

    fun lastUserMessageId(): Int? = sentMessages.keys.lastOrNull()

    fun getUserBlocks(userId: Long) = dbProvider.getUserBlocks(userId)

    fun getBlockPages(blockId: Long) = dbProvider.getBlockPages(blockId)

    fun getLastBlockPages(userId: Long) = dbProvider.findLastBlockByUserId(userId)
        ?.let { dbProvider.getBlockPages(it.id) } ?: emptyList()

    override fun sendMessage(
        chatId: String,
        message: String,
        contentType: ContentType,
        disablePreview: Boolean,
        preSendHandler: Consumer<SendMessage>
    ): Int {
        // TODO: check method was called
        log.info("sendMessage: chatId: $chatId, message: $message")

        val messageId = messageIdCounter.incrementAndGet()
        sentMessages[messageId] = message

        return messageId
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

    override fun sendDocument(
        chatId: String,
        file: File,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override fun sendVideo(
        chatId: String,
        fileId: String,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override fun sendVideo(
        chatId: String,
        file: File,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override fun sendImages(
        chatId: String,
        files: Array<String>,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override fun sendImages(
        chatId: String,
        files: List<File>,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override fun sendImage(
        chatId: String,
        file: File,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override fun deleteMessage(chatId: String, messageId: Int) {
        TODO("Not yet implemented")
    }

    override fun executed(command: BaseCommand, messageType: MessageType) {
        command::class.call()
    }

    private fun getLocalizationProvider(userId: Long): TestUserLocalizationProvider =
        localProviders.computeIfAbsent(userId) {
            TestUserLocalizationProvider(userId)
        }
}

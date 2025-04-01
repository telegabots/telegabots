package org.github.telegabots.exectutor

import org.github.telegabots.api.*
import org.github.telegabots.api.config.BotConfig
import org.github.telegabots.entity.CommandBlock
import org.github.telegabots.entity.CommandPage
import org.github.telegabots.service.LanguageImpl
import org.github.telegabots.state.MemoryStateDbProvider
import org.github.telegabots.state.StateDbProvider
import org.github.telegabots.test.TestLocalizationProvider
import org.github.telegabots.test.call
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

/**
 * Test executor for bot commands
 */
class BotCommandExecutor(private val rootCommand: Class<out BaseCommand>, services: List<Service>) : MessageSender, CommandInterceptor {
    private val log = LoggerFactory.getLogger(BotCommandExecutor::class.java)!!
    private val messageIdCounter = AtomicInteger(100_000)
    private val serviceProvider = mock(ServiceProvider::class.java)
    private val dbProvider = MemoryStateDbProvider()
    private val localizationFactory = mock(LocalizationFactory::class.java)
    private val localProvider = TestLocalizationProvider()
    private val telegaBot: TelegaBot
    private val sentMessages = mutableMapOf<Int, String>()
    private val config = BotConfig.load(Properties())

    init {
        doReturn(localizationFactory).`when`(serviceProvider).tryGetService(LocalizationFactory::class.java)
        doReturn(this).`when`(serviceProvider).tryGetService(CommandInterceptor::class.java)
        doReturn(dbProvider).`when`(serviceProvider).tryGetService(StateDbProvider::class.java)
        doReturn(localProvider).`when`(localizationFactory).getProvider(anyString())
        doReturn(listOf(LanguageImpl.ENGLISH)).`when`(localizationFactory).getSupportedLanguages()
        services.forEach { service ->
            doReturn(service).`when`(serviceProvider).tryGetService(service.javaClass)
        }

        telegaBot = TelegaBot(
            messageSender = this,
            serviceProvider = serviceProvider,
            config = config,
            rootCommand = rootCommand
        )
    }

    fun handle(update: Update): Boolean {
        return telegaBot.handle(update)
    }

    fun addService(service: Class<out Service>, instance: Service) {
        Mockito.`when`(serviceProvider.tryGetService(service)).thenReturn(instance)
    }

    fun addLocalization(userId: Long, vararg localPairs: Pair<String, String>) {
        localProvider.addLocalization(*localPairs)
    }

    fun lastUserMessageId(): Int? = sentMessages.keys.lastOrNull()

    fun getUserBlocks(userId: Long): List<CommandBlock>  = dbProvider.getUserBlocks(userId)

    fun getBlockPages(blockId: Long): List<CommandPage> = dbProvider.getBlockPages(blockId)

    fun getLastBlock(userId: Long): CommandBlock? = dbProvider.findLastBlockByUserId(userId)

    fun getBlockByMessage(userId: Long, messageId: Int): CommandBlock? = dbProvider.findBlockByMessageId(userId, messageId)

    fun getLastBlockPages(userId: Long): List<CommandPage> = getLastBlock(userId)
        ?.let { getBlockPages(it.id) } ?: emptyList()

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

    override fun executed(command: BaseCommand, messageType: MessageType, result: Boolean) {
        command::class.call()
    }
}

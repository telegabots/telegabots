package org.github.telegabots

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.exectutor.BotCommandExecutor
import org.mockito.Mockito
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import java.time.LocalDateTime
import java.util.*

abstract class BaseTests {
    protected fun createExecutor(clazz: Class<out BaseCommand>) = BotCommandExecutor(rootCommand = clazz)
    private val random = Random()

    protected fun nextRandomInt(): Int = random.nextInt()

    protected fun nextRandomLong(): Long = random.nextLong()

    protected fun createAnyMessage(
        messageText: String = "Message does not matter " + LocalDateTime.now(),
        chatId: Long = random.nextLong(),
        userId: Int = random.nextInt()
    ): Update = createMessage(messageText, chatId, userId)

    protected fun createAnyCallbackMessage(
        messageId: Int = random.nextInt(),
        callbackData: String = "make_jvm_great_again_" + System.currentTimeMillis(),
        chatId: Long = random.nextLong(),
        userId: Int = random.nextInt()
    ): Update = createCallbackMessage(messageId, callbackData, chatId, userId)

    private fun createMessage(messageText: String, chatId: Long, userId: Int): Update {
        val fromUser = Mockito.mock(User::class.java)
        Mockito.`when`(fromUser.id).thenReturn(userId)
        val message = Mockito.mock(Message::class.java)
        Mockito.`when`(message.hasText()).thenReturn(true)
        Mockito.`when`(message.text).thenReturn(messageText)
        Mockito.`when`(message.chatId).thenReturn(chatId)
        Mockito.`when`(message.from).thenReturn(fromUser)
        val update = Mockito.mock(Update::class.java)
        Mockito.`when`(update.hasMessage()).thenReturn(true)
        Mockito.`when`(update.message).thenReturn(message)
        return update
    }

    private fun createCallbackMessage(messageId: Int, callbackData: String, chatId: Long, userId: Int): Update {
        val fromUser = Mockito.mock(User::class.java)
        Mockito.`when`(fromUser.id).thenReturn(userId)
        val message = Mockito.mock(Message::class.java)
        Mockito.`when`(message.hasText()).thenReturn(false)
        Mockito.`when`(message.chatId).thenReturn(chatId)
        Mockito.`when`(message.from).thenReturn(fromUser)
        Mockito.`when`(message.messageId).thenReturn(messageId)
        val update = Mockito.mock(Update::class.java)
        Mockito.`when`(update.hasMessage()).thenReturn(false)
        Mockito.`when`(update.hasCallbackQuery()).thenReturn(true)
        val callbackQuery = Mockito.mock(CallbackQuery::class.java)
        Mockito.`when`(update.callbackQuery).thenReturn(callbackQuery)
        Mockito.`when`(callbackQuery.message).thenReturn(message)
        Mockito.`when`(callbackQuery.data).thenReturn(callbackData)
        return update
    }
}

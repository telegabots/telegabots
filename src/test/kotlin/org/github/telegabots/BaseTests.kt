package org.github.telegabots

import org.github.telegabots.exectutor.BotCommandExecutor
import org.mockito.Mockito
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User

abstract class BaseTests {
    protected fun createExecutor(clazz: Class<out BaseCommand>) = BotCommandExecutor(rootCommand = clazz)

    protected fun createAnyMessage(
        messageText: String = "Message does not matter",
        chatId: Long = -123456L,
        userId: Int = 66642555
    ): Update = createMessage(messageText, chatId, userId)

    protected fun createAnyCallbackMessage(
        messageId: Int = 987654321,
        callbackData: String = "make_jvm_great_again",
        chatId: Long = -55776699,
        userId: Int = 456999777
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

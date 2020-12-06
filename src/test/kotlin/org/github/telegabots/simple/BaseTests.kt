package org.github.telegabots.simple

import org.mockito.Mockito
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User

abstract class BaseTests {
    protected fun createAnyMessage(): Update = createMessage("Message does not matter", -123456L, 66642555)

    protected fun createMessage(text: String, chatId: Long, userId: Int): Update {
        val message = Mockito.mock(Message::class.java)
        Mockito.`when`(message.hasText()).thenReturn(true)
        Mockito.`when`(message.text).thenReturn(text)
        Mockito.`when`(message.chatId).thenReturn(chatId)
        val fromUser = Mockito.mock(User::class.java)
        Mockito.`when`(fromUser.id).thenReturn(userId)
        Mockito.`when`(message.from).thenReturn(fromUser)
        val update = Mockito.mock(Update::class.java)
        Mockito.`when`(update.hasMessage()).thenReturn(true)
        Mockito.`when`(update.message).thenReturn(message)
        return update
    }
}

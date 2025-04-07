package org.github.telegabots.api

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import java.io.File
import java.util.function.Consumer

/**
 * Message sender of Telegram
 */
interface MessageSender : Service {
    /**
     * Sends new message
     */
    fun sendMessage(
        chatId: String,
        message: String,
        contentType: ContentType = ContentType.Plain,
        disablePreview: Boolean = false,
        preSendHandler: Consumer<SendMessage>
    ): Int

    fun sendMessage(
        chatId: String,
        message: String,
        contentType: ContentType = ContentType.Plain
    ): Int = sendMessage(chatId, message, contentType, false) { }

    /**
     * Updates existing message
     */
    fun updateMessage(
        chatId: String,
        messageId: Int,
        message: String,
        contentType: ContentType = ContentType.Plain,
        disablePreview: Boolean = false,
        preSendHandler: Consumer<EditMessageText> = Consumer { }
    )

    fun updateMessage(
        chatId: String,
        messageId: Int,
        message: String,
        contentType: ContentType = ContentType.Plain
    ) = updateMessage(chatId, messageId, message, contentType, false) { }

    /**
     * Sends file to the chat
     */
    fun sendDocument(
        chatId: String,
        file: File,
        caption: String,
        captionContentType: ContentType = ContentType.Plain,
        disableNotification: Boolean = false
    )

    /**
     * Sends video to the chat
     */
    fun sendVideo(
        chatId: String,
        fileId: String,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    )

    /**
     * Sends video to the chat
     */
    fun sendVideo(
        chatId: String,
        file: File,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    )

    /**
     * Sends images to the chat
     */
    fun sendImages(
        chatId: String,
        files: Array<String>,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    )

    /**
     * Sends images to the chat
     */
    fun sendImages(
        chatId: String,
        files: List<File>,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    )

    /**
     * Sends image to the chat
     */
    fun sendImage(
        chatId: String,
        file: File,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    ): Int

    fun updateImage(
        chatId: String,
        messageId: Int,
        file: File,
        caption: String,
        captionContentType: ContentType
    )

    fun deleteMessage(chatId: String, messageId: Int)
}
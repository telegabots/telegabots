package org.github.telegabots.service

import org.github.telegabots.api.ContentType
import org.github.telegabots.api.MessageSender
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.*
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.io.File
import java.io.Serializable
import java.util.function.Consumer

class MessageSenderImpl(private val bot: TelegramLongPollingBot) : MessageSender {
    private val log = LoggerFactory.getLogger(javaClass)!!

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
            val resp = bot.execute<Message, SendMessage>(sendMessage)
            return resp.messageId
        } catch (e: Exception) {
            log.error("send message failed: {}, message: {}", e.message, sendMessage, e)
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
            bot.execute<Serializable, EditMessageText>(editMessageText)
        } catch (e: Exception) {
            log.error("edit message failed: {}, message: {}", e.message, editMessageText, e)
            throw e
        }
    }

    override fun sendDocument(
        chatId: String,
        file: File,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    ) {
        val doc = SendDocument()
        doc.chatId = chatId
        doc.setDocument(file)

        doc.parseMode = when (captionContentType) {
            ContentType.Markdown -> ParseMode.MARKDOWN
            ContentType.Html -> ParseMode.HTML
            ContentType.Plain -> doc.parseMode
        }

        if (disableNotification) {
            doc.disableNotification()
        }

        if (caption.isNotBlank()) {
            doc.caption = caption
        }

        try {
            log.info("Sending file: {}", doc)
            bot.execute(doc)
        } catch (e: TelegramApiException) {
            log.error("Document send failed: {}, document: {}", e.message, doc, e)
            throw e
        }
    }

    override fun sendVideo(
        chatId: String,
        fileId: String,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    ) {
        val video = SendVideo()
        video.setVideo(fileId)

        sendVideoInternal(video, chatId, captionContentType, disableNotification, caption)
    }

    override fun sendVideo(
        chatId: String,
        file: File,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    ) {
        val video = SendVideo()
        video.setVideo(file)

        sendVideoInternal(video, chatId, captionContentType, disableNotification, caption)
    }

    override fun sendImages(
        chatId: String,
        files: Array<String>,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    ) {
        val images = SendMediaGroup()
        images.media = files.map { imageIds -> InputMediaPhoto().setMedia(imageIds) }

        sendImagesInternal(images, chatId, disableNotification, caption, captionContentType)
    }

    override fun sendImages(
        chatId: String,
        files: List<File>,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    ) {
        if (files.size == 1) {
            sendImage(chatId, files.first(), caption, captionContentType, disableNotification)
            return
        }

        val images = SendMediaGroup()
        images.media = files.map { file -> InputMediaPhoto().setMedia(file, file.name) }

        sendImagesInternal(images, chatId, disableNotification, caption, captionContentType)
    }

    override fun sendImage(
        chatId: String,
        file: File,
        caption: String,
        captionContentType: ContentType,
        disableNotification: Boolean
    ) {
        val image = SendPhoto()
        image.chatId = chatId
        image.setPhoto(file)

        if (disableNotification) {
            image.disableNotification()
        }

        if (caption.isNotBlank()) {
            image.caption = caption
            image.parseMode = getParseMode(captionContentType, image.parseMode)
        }

        try {
            log.info("Sending image: {}", image)
            bot.execute(image)
        } catch (e: TelegramApiException) {
            log.error("Image send failed: {}, image: {}", e.message, image, e)
            throw e
        }
    }

    private fun sendImagesInternal(
        images: SendMediaGroup,
        chatId: String,
        disableNotification: Boolean,
        caption: String,
        captionContentType: ContentType
    ) {
        images.chatId = chatId

        if (disableNotification) {
            images.disableNotification()
        }

        if (caption.isNotBlank()) {
            val first = images.media.first()
            first.caption = caption
            first.parseMode = getParseMode(captionContentType, first.parseMode)
        }

        try {
            log.info("Sending images: {}", images)
            bot.execute(images)
        } catch (e: TelegramApiException) {
            log.error("Images send failed: {}, images: {}", e.message, images, e)
            throw e
        }
    }

    private fun sendVideoInternal(
        video: SendVideo,
        chatId: String,
        captionContentType: ContentType,
        disableNotification: Boolean,
        caption: String
    ) {
        video.chatId = chatId

        if (disableNotification) {
            video.disableNotification()
        }

        if (caption.isNotBlank()) {
            video.caption = caption
            video.parseMode = getParseMode(captionContentType, video.parseMode)
        }

        try {
            log.info("Sending video: {}", video)
            bot.execute(video)
        } catch (e: TelegramApiException) {
            log.error("Video send failed: {}, video: {}", e.message, video, e)
            throw e
        }
    }

    private fun getParseMode(captionContentType: ContentType, oldParseMode: String) = when (captionContentType) {
        ContentType.Markdown -> ParseMode.MARKDOWN
        ContentType.Html -> ParseMode.HTML
        ContentType.Plain -> oldParseMode
    }
}
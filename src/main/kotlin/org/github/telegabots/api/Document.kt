package org.github.telegabots.api

import org.github.telegabots.MessageFile

data class Document(
    val file: MessageFile,
    val caption: String = "",
    val captionContentType: ContentType = ContentType.Plain,
    val disableNotification: Boolean = false,
    val chatId: String = ""
) {
    companion object {
        @JvmStatic
        fun of(
            file: MessageFile,
            caption: String = "",
            captionContentType: ContentType = ContentType.Plain,
            disableNotification: Boolean = false,
            chatId: String = ""
        ) =
            Document(
                file = file,
                caption = caption,
                captionContentType = captionContentType,
                disableNotification = disableNotification,
                chatId = chatId
            )
    }
}

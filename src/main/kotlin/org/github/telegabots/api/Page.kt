package org.github.telegabots.api

import org.github.telegabots.MessageFile

data class Page(
    val message: String,
    val contentType: ContentType = ContentType.Plain,
    val messageType: MessageType = MessageType.Text,
    val disablePreview: Boolean = false,
    val disableNotification: Boolean = false,
    val subCommands: List<List<SubCommand>> = emptyList(),
    val handler: Class<out BaseCommand>? = null,
    val id: Long = 0L,
    val blockId: Long = 0L,
    val state: StateRef? = null,
    val file: MessageFile? = null,
) {
    override fun toString(): String {
        return "Page(contentType=$contentType, messageType=$messageType, disablePreview=$disablePreview, subCommands=$subCommands, handler=$handler, id=$id,\nmessage='${
            message.take(
                TO_STR_MESSAGE_LEN
            )
        }')"
    }

    companion object {
        private const val TO_STR_MESSAGE_LEN = 25
    }
}

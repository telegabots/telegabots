package org.github.telegabots.api

import org.github.telegabots.MessageFile

data class Page(
    val message: String,
    val contentType: ContentType = ContentType.Plain,
    val messageType: MessageType = MessageType.Text,
    val disablePreview: Boolean = false,
    val disableNotification: Boolean = false,
    val buttons: List<List<Button>> = emptyList(),
    val handler: Class<out BaseController>? = null,
    val id: Long = 0L,
    val blockId: Long = 0L,
    val state: StateRef? = null,
    val file: MessageFile? = null,
    /**
     * Enable back button for [Page], if page is not first page of the block.
     */
    val enableBack: Boolean? = null,
) {
    override fun toString(): String {
        return "Page(contentType=$contentType, messageType=$messageType, disablePreview=$disablePreview, buttons=$buttons, handler=$handler, id=$id,\nmessage='${
            message.take(
                TO_STR_MESSAGE_LEN
            )
        }')"
    }

    companion object {
        private const val TO_STR_MESSAGE_LEN = 25
    }
}

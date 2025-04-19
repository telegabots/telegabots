package org.github.telegabots.api

/**
 * Input message from the user
 */
data class InputMessage(
    val type: MessageType,
    val query: String,
    val chatId: Long,
    val userId: Long,
    val isAdmin: Boolean,
    val user: InputUser,
    val messageId: Int,
    val inlineMessageId: Int?
) {
    init {
        check(userId != 0L) { "UserId cannot be $userId" }
        check(chatId != 0L) { "ChatId cannot be $chatId" }
    }

    fun toInputRefresh() = this.copy(query = SystemCommands.REFRESH)
}

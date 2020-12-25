package org.github.telegabots.api.entity

import org.github.telegabots.api.MessageType

/**
 * Entity related with message and user
 */
data class CommandBlock(val id: Long,
                        val messageId: Int,
                        val userId: Int,
                        val messageType: MessageType
) {
    fun isValid(): Boolean = messageId != 0 && userId != 0
}

package org.github.telegabots.entity

import org.github.telegabots.api.MessageType
import java.time.LocalDateTime

/**
 * Entity related with message and user
 */
data class CommandBlock(val id: Long,
                        val messageId: Int,
                        val userId: Int,
                        val messageType: MessageType,
                        val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun isValid(): Boolean = messageId != 0 && userId != 0
}

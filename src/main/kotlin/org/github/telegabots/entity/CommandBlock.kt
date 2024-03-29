package org.github.telegabots.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import org.github.telegabots.api.MessageType
import java.time.LocalDateTime

/**
 * Entity related with message and user
 */
data class CommandBlock(val messageId: Int,
                        val userId: Long,
                        val messageType: MessageType,
                        val id: Long = 0L,
                        val createdAt: LocalDateTime = LocalDateTime.now()
) {
    @JsonIgnore
    fun isValid(): Boolean = messageId != 0 && userId != 0L
}

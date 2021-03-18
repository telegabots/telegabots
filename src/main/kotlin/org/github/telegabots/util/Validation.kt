package org.github.telegabots.util

import java.lang.IllegalArgumentException

object Validation {
    fun validateMessageId(messageId: Int) {
        if (messageId == 0) {
            throw IllegalArgumentException("MessageId cannot be equals to $messageId")
        }
    }
}

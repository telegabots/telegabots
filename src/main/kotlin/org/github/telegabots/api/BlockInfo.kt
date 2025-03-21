package org.github.telegabots.api

import java.time.LocalDateTime

/**
 * Info about block
 */
data class BlockInfo(
    val id: Long,
    val createdAt: LocalDateTime
)

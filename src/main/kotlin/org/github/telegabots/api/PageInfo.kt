package org.github.telegabots.api

import java.time.LocalDateTime

/**
 * Info about page
 */
data class PageInfo(
    val id: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

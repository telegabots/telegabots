package org.github.telegabots.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object TimeUtil {
    fun fromEpochMillis(millis: Long): LocalDateTime =
        Instant.ofEpochMilli(millis)
            .atZone(DEFAULT_ZONE)
            .toLocalDateTime()

    fun toEpochMillis(time: LocalDateTime): Long =
        time.atZone(DEFAULT_ZONE)
            .toInstant()
            .toEpochMilli()

    private val DEFAULT_ZONE = ZoneId.systemDefault()
}

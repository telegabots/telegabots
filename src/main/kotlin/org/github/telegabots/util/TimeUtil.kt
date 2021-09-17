package org.github.telegabots.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object TimeUtil {
    fun fromEpochToLocal(millis: Long): LocalDateTime =
        Instant.ofEpochMilli(millis)
            .atZone(DEFAULT_ZONE)
            .toLocalDateTime()


    private val DEFAULT_ZONE = ZoneId.systemDefault()
}

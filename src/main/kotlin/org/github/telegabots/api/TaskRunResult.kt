package org.github.telegabots.api

import java.time.LocalDateTime

data class TaskRunResult(
    val task: BaseTask,
    val startedTime: LocalDateTime,
    val runningTime: Long,
    val result: Any? = null,
    val error: Exception? = null
)

package org.github.telegabots.task

import org.github.telegabots.api.BaseTask
import org.github.telegabots.api.Task
import org.github.telegabots.api.TaskState
import java.time.LocalDateTime
import java.util.concurrent.ExecutorService

class TaskWrapper(
    private val task: BaseTask,
    private val startedTime: LocalDateTime,
    private val executorService: ExecutorService
) : Task {
    private var state = TaskState.Initted

    override fun state(): TaskState = state

    override fun run() {
        TODO("Not yet implemented")
    }

    override fun stop() {
        if (state != TaskState.Stopping || state != TaskState.Stopped) {
            state = TaskState.Stopping
            task.stopAsync()
        }
    }

    override fun status(): String? = task.status()

    override fun startedTime(): LocalDateTime = startedTime

    override fun estimateEndTime(): LocalDateTime? = task.estimateEndTime()

    override fun progress(): Int? = task.progress()
}

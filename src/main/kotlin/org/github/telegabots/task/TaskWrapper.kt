package org.github.telegabots.task

import org.github.telegabots.api.BaseTask
import org.github.telegabots.api.Task
import org.github.telegabots.api.TaskState
import java.time.LocalDateTime
import java.util.concurrent.Future

class TaskWrapper(
    private val task: BaseTask,
    private val startedTime: LocalDateTime,
    private val future: Future<Any>
) : Task {
    override fun state(): TaskState {
        TODO("Not yet implemented")
    }

    override fun stopAsync() {
        future.cancel(true)
    }

    override fun status(): String? = task.status()

    override fun startedTime(): LocalDateTime = startedTime

    override fun estimateEndTime(): LocalDateTime? = task.estimateEndTime()

    override fun progress(): Int? = task.progress()
}

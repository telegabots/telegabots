package org.github.telegabots.task

import org.github.telegabots.api.BaseTask
import org.github.telegabots.api.Task
import org.github.telegabots.api.TaskState
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.concurrent.ExecutorService

class TaskWrapper(
    private val task: BaseTask,
    private val startedTime: LocalDateTime,
    private val executorService: ExecutorService
) : Task {
    private val log = LoggerFactory.getLogger(javaClass)!!
    private var state = TaskState.Initted
    private val runner = TaskRunner(task) { tsk: BaseTask, error: Exception? -> taskStopHandler(tsk, error) }

    override fun id(): String = task.id()

    override fun title(): String = task.title()

    override fun state(): TaskState = state

    @Synchronized
    override fun run() {
        if (state == TaskState.Initted || state == TaskState.Stopped) {
            state = TaskState.Starting
            executorService.submit(runner)
        } else {
            log.info("Task '{}' cannot be started. State is {}", task.id(), state)
        }
    }

    @Synchronized
    override fun stop() {
        if (state == TaskState.Started) {
            state = TaskState.Stopping
            task.stopAsync()
        } else {
            log.info("Task '{}' cannot be stopped. State is {}", task.id(), state)
        }
    }

    @Synchronized
    private fun taskStopHandler(tsk: BaseTask, error: Exception?) {
        state = TaskState.Stopped
    }

    override fun status(): String? = task.status()

    override fun startedTime(): LocalDateTime = startedTime

    override fun estimateEndTime(): LocalDateTime? = task.estimateEndTime()

    override fun progress(): Int? = task.progress()
}

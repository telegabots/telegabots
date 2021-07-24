package org.github.telegabots.task

import org.github.telegabots.api.BaseTask
import org.github.telegabots.api.Task
import org.github.telegabots.api.TaskState
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.concurrent.ExecutorService

class TaskWrapper(
    private val task: BaseTask,
    private val executorService: ExecutorService
) : Task {
    private val log = LoggerFactory.getLogger(javaClass)!!

    @Volatile
    private var state = TaskState.Initted
    private val runner = TaskRunner(task) { info: TaskRunInfo, error: Exception? -> taskStopHandler(info, error) }

    override fun id(): String = task.id()

    override fun title(): String = task.title()

    override fun state(): TaskState = state

    @Synchronized
    override fun run() {
        if (state == TaskState.Initted || state == TaskState.Stopped) {
            state = TaskState.Starting
            log.debug("Task submit to run '{}'", task.id())
            executorService.submit(runner)
        } else {
            log.info("Task '{}' cannot be started. State is {}", task.id(), state)
        }
    }

    @Synchronized
    override fun stop() {
        if (state == TaskState.Started) {
            state = TaskState.Stopping
            log.debug("Try to stop task '{}'", task.id())
            task.stopAsync()
        } else {
            log.info("Task '{}' cannot be stopped. State is {}", task.id(), state)
        }
    }

    @Synchronized
    private fun taskStopHandler(info: TaskRunInfo, error: Exception?) {
        val byUser = state == TaskState.Stopping
        state = TaskState.Stopped
        val task = info.task

        when {
            error != null -> {
                log.error(
                    "Task '{}' stopped after {} ms with error: {}",
                    task.id(),
                    info.runningTime,
                    error.message,
                    error
                )
            }
            byUser -> {
                log.debug("Task '{}' stopped (byUser) after {} ms", task.id(), info.runningTime)
            }
            else -> {
                log.debug("Task '{}' stopped (self) after {} ms", task.id(), info.runningTime)
            }
        }
    }

    override fun status(): String? = task.status()

    override fun startedTime(): LocalDateTime? = runner.startedTime

    override fun estimateEndTime(): LocalDateTime? = task.estimateEndTime()

    override fun progress(): Int? = task.progress()
}

package org.github.telegabots.task

import org.github.telegabots.api.BaseTask
import org.github.telegabots.api.Task
import org.github.telegabots.api.TaskContext
import org.github.telegabots.api.TaskRunResult
import org.github.telegabots.api.TaskState
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.concurrent.ExecutorService
import java.util.function.Consumer

class TaskWrapper(
    private val task: BaseTask,
    private val context: TaskContext,
    private val executorService: ExecutorService
) : Task {
    private val log = LoggerFactory.getLogger(javaClass)!!

    @Volatile
    private var startedTime: LocalDateTime? = null

    @Volatile
    private var state = TaskState.Initted

    override fun id(): String = task.id()

    override fun title(): String = task.title()

    override fun state(): TaskState = state

    @Synchronized
    override fun start(onComplete: Consumer<TaskRunResult>?) {
        if (state == TaskState.Initted || state == TaskState.Stopped) {
            state = TaskState.Starting
            log.debug("Task submit to run '{}'", task.id())

            executorService.submit(
                TaskRunner(
                    task,
                    context,
                    taskStartHandler = { taskStartHandler(it) },
                    taskStoppedHandler = { taskStopHandler(it, onComplete) })
            )
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
    private fun taskStartHandler(startedTime: LocalDateTime) {
        this.startedTime = startedTime
        state = TaskState.Started
    }

    @Synchronized
    private fun taskStopHandler(info: TaskRunResult, onComplete: Consumer<TaskRunResult>?) {
        val byUser = state == TaskState.Stopping
        state = TaskState.Stopped
        val task = info.task
        val error = info.error

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

        try {
            onComplete?.accept(info)
        } catch (e: Exception) {
            log.error("Error in task onComplete handler: {}", e.message, e)
        }
    }

    override fun status(): String? = task.status()

    override fun startedTime(): LocalDateTime? = startedTime

    override fun estimateEndTime(): LocalDateTime? = task.estimateEndTime()

    override fun progress(): Int? = task.progress()
}

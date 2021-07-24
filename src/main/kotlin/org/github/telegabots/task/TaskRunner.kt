package org.github.telegabots.task

import org.github.telegabots.api.BaseTask
import java.time.LocalDateTime
import java.util.concurrent.Callable
import java.util.function.BiConsumer

/**
 * Task routine class
 */
class TaskRunner(
    private val task: BaseTask,
    private val taskStoppedHandler: BiConsumer<TaskRunInfo, Exception?>
) : Callable<Any?> {
    @Volatile
    var startedTime: LocalDateTime? = null
        private set
    @Volatile
    var called = false
        private set

    override fun call(): Any? {
        val startTime = System.currentTimeMillis()
        startedTime = LocalDateTime.now()

        try {
            task.run()
            called = true
            taskStoppedHandler.accept(TaskRunInfo(task, startedTime!!, System.currentTimeMillis() - startTime), null)
            startedTime = null
        } catch (e: java.lang.Exception) {
            called = true
            taskStoppedHandler.accept(TaskRunInfo(task, startedTime!!, System.currentTimeMillis() - startTime), e)
            startedTime = null
        }

        return null
    }
}

data class TaskRunInfo(val task: BaseTask, val startedTime: LocalDateTime, val runningTime: Long)

package org.github.telegabots.task

import org.github.telegabots.api.BaseTask
import org.github.telegabots.api.TaskRunResult
import java.time.LocalDateTime
import java.util.concurrent.Callable
import java.util.function.Consumer

/**
 * Task routine class
 */
class TaskRunner(
    private val task: BaseTask,
    private val taskStartHandler: Consumer<LocalDateTime>,
    private val taskStoppedHandler: Consumer<TaskRunResult>
) : Callable<Any?> {

    override fun call(): Any? {
        val startTime = System.currentTimeMillis()
        val startedTime = LocalDateTime.now()

        try {
            taskStartHandler.accept(startedTime)
            task.run()
            taskStoppedHandler.accept(TaskRunResult(task, startedTime, System.currentTimeMillis() - startTime))
        } catch (e: Exception) {
            taskStoppedHandler.accept(TaskRunResult(task, startedTime, System.currentTimeMillis() - startTime, e))
        }

        return null
    }
}

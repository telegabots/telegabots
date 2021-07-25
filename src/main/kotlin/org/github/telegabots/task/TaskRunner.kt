package org.github.telegabots.task

import org.github.telegabots.api.BaseTask
import java.util.concurrent.Callable
import java.util.function.BiConsumer

/**
 * Task routine class
 */
class TaskRunner(
    private val task: BaseTask,
    private val taskStartHandler: Runnable,
    private val taskStoppedHandler: BiConsumer<TaskRunInfo, Exception?>
) : Callable<Any?> {

    override fun call(): Any? {
        val startTime = System.currentTimeMillis()

        try {
            taskStartHandler.run()
            task.run()
            taskStoppedHandler.accept(TaskRunInfo(task, System.currentTimeMillis() - startTime), null)
        } catch (e: java.lang.Exception) {
            taskStoppedHandler.accept(TaskRunInfo(task, System.currentTimeMillis() - startTime), e)
        }

        return null
    }
}

data class TaskRunInfo(val task: BaseTask, val runningTime: Long)

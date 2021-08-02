package org.github.telegabots.task

import org.github.telegabots.api.BaseTask
import org.github.telegabots.api.TaskContext
import org.github.telegabots.api.TaskRunResult
import org.github.telegabots.context.TaskContextSupport
import java.time.LocalDateTime
import java.util.concurrent.Callable
import java.util.function.Consumer
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Task routine class
 */
class TaskRunner(
    private val task: BaseTask,
    private val context: TaskContext,
    private val taskStartHandler: Consumer<LocalDateTime>,
    private val taskStoppedHandler: Consumer<TaskRunResult>
) : Callable<Any?> {

    override fun call(): Any? {
        val startTime = System.currentTimeMillis()
        val startedTime = LocalDateTime.now()

        try {
            setContext(task, context)
            taskStartHandler.accept(startedTime)
            task.run()
            taskStoppedHandler.accept(TaskRunResult(task, startedTime, System.currentTimeMillis() - startTime))
            clearContext(task)
        } catch (e: Exception) {
            taskStoppedHandler.accept(TaskRunResult(task, startedTime, System.currentTimeMillis() - startTime, e))
            clearContext(task)
        }

        return null
    }

    companion object {
        /**
         * Sets current context of task
         */
        private fun setContext(task: BaseTask, context: TaskContext?) {
            val prop = BaseTask::class.memberProperties.find { it.name == "context" }
                ?: throw IllegalStateException("Context not found in task: ${task.javaClass.name}")
            prop.isAccessible = true
            (prop.get(task) as TaskContextSupport).setContext(context)
        }

        private fun clearContext(task: BaseTask) {
            setContext(task, null)
        }
    }
}

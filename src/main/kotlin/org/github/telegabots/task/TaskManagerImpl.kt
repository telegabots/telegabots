package org.github.telegabots.task

import org.github.telegabots.api.BaseTask
import org.github.telegabots.api.Task
import org.github.telegabots.api.TaskContext
import org.github.telegabots.api.TaskManager
import org.github.telegabots.context.TaskContextSupport
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class TaskManagerImpl(val context: TaskContext) : TaskManager {
    private val log = LoggerFactory.getLogger(javaClass)!!
    private val executorService = Executors.newCachedThreadPool()
    private val tasks = mutableSetOf<TaskWrapper>()

    override fun register(task: BaseTask): Task {
        val wrapper = TaskWrapper(task, executorService)

        synchronized(tasks) {
            tasks.add(wrapper)
            setContext(task, context)
        }

        log.info("Task '{}' registered", task.id())

        return wrapper
    }

    override fun unregister(task: Task) {
        synchronized(tasks) {
            val wrapper = task as TaskWrapper

            if (!tasks.contains(wrapper)) {
                return
            }

            // TODO: stop task if running. sync wait until stop and then remove
            clearContext(wrapper.task)
            tasks.remove(wrapper)

            log.info("Task '{}' unregistered", task.id())
        }
    }

    override fun getAll(): List<Task> =
        synchronized(tasks) {
            tasks.toList()
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

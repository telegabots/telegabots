package org.github.telegabots.task

import org.github.telegabots.api.BaseTask
import org.github.telegabots.api.Task
import org.github.telegabots.api.TaskContext
import org.github.telegabots.api.TaskManager
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService

class TaskManagerImpl(
    private val context: TaskContext,
    private val executorService: ExecutorService,
    private val tasks: MutableSet<Task>
) : TaskManager {
    private val log = LoggerFactory.getLogger(javaClass)!!

    override fun register(task: BaseTask): Task {
        val wrapper = TaskWrapper(task, context, executorService)

        synchronized(tasks) {
            tasks.add(wrapper)
        }

        log.info("Task '{}' registered", task.id())

        return wrapper
    }

    override fun unregister(task: Task) {
        synchronized(tasks) {
            if (!tasks.contains(task)) {
                log.warn("Task '{}' not registered. Unregister skipped", task.id())
                return
            }

            // TODO: stop task if running. sync wait until stop and then remove
            tasks.remove(task)

            log.info("Task '{}' unregistered", task.id())
        }
    }

    override fun getAll(): List<Task> =
        synchronized(tasks) {
            tasks.toList()
        }
}

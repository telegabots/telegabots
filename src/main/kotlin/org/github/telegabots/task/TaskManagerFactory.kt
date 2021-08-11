package org.github.telegabots.task

import org.github.telegabots.api.ServiceProvider
import org.github.telegabots.api.Task
import org.github.telegabots.api.TaskContext
import org.github.telegabots.api.TaskManager
import org.github.telegabots.service.CommandContextImpl
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

/**
 * Factory that creates TaskManager related with blockId/pageId
 */
class TaskManagerFactory(val serviceProvider: ServiceProvider) {
    private val log = LoggerFactory.getLogger(javaClass)!!
    private val executorService = Executors.newCachedThreadPool()
    private val tasks = mutableSetOf<Task>()

    fun create(context: TaskContext): TaskManager {
        if (log.isDebugEnabled) {
            val realContext = context as? CommandContextImpl

            log.debug(
                "Create taskManager, blockId: {}, pageId: {}, messageId: {}, user: {}",
                context.blockId(), context.pageId(), context.messageId(), realContext?.user()
            )
        }

        return TaskManagerImpl(context, executorService, tasks)
    }
}

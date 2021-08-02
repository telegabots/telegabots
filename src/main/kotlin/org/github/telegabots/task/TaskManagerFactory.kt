package org.github.telegabots.task

import org.github.telegabots.api.InputUser
import org.github.telegabots.api.ServiceProvider
import org.github.telegabots.api.TaskManager
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

/**
 * Factory that creates TaskManager related with blockId/pageId
 */
class TaskManagerFactory(val serviceProvider: ServiceProvider) {
    private val log = LoggerFactory.getLogger(javaClass)!!
    private val executorService = Executors.newCachedThreadPool()

    fun create(blockId: Long, pageId: Long, user: InputUser): TaskManager {
        if (log.isDebugEnabled) {
            log.debug("Create taskManager, blockId: {}, pageId: {}, user: {}", blockId, pageId, user)
        }

        val context = TaskContextImpl(blockId, pageId, user, serviceProvider)
        return TaskManagerImpl(context, executorService)
    }
}

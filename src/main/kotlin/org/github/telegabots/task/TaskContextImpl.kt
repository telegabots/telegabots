package org.github.telegabots.task

import org.github.telegabots.api.InputUser
import org.github.telegabots.api.Service
import org.github.telegabots.api.ServiceProvider
import org.github.telegabots.api.StateRef
import org.github.telegabots.api.TaskContext
import org.github.telegabots.api.UserService

class TaskContextImpl(
    private val blockId: Long,
    private val pageId: Long,
    private val user: InputUser,
    private val serviceProvider: ServiceProvider
) : TaskContext {
    override fun blockId(): Long = blockId

    override fun pageId(): Long = pageId

    override fun refreshPage(pageId: Long, state: StateRef?) {
        TODO("Not yet implemented")
    }

    override fun <T : Service> getService(clazz: Class<T>): T? = serviceProvider.getService(clazz)

    override fun <T : UserService> getUserService(clazz: Class<T>): T? = serviceProvider.getUserService(clazz, user)
}

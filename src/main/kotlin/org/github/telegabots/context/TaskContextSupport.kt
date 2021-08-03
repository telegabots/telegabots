package org.github.telegabots.context

import org.github.telegabots.api.Service
import org.github.telegabots.api.StateRef
import org.github.telegabots.api.TaskContext
import org.github.telegabots.api.UserService

object TaskContextSupport : TaskContext {
    private val contextCurrent = ThreadLocal<TaskContext>()

    internal fun setContext(context: TaskContext?) =
        if (context != null)
            contextCurrent.set(context)
        else
            contextCurrent.remove()

    private fun current(): TaskContext {
        return contextCurrent.get()
            ?: throw IllegalStateException("Task context not initialized for current task")
    }

    override fun blockId(): Long = current().blockId()

    override fun pageId(): Long = current().pageId()

    override fun refreshPage(pageId: Long, state: StateRef?) = current().refreshPage(pageId, state)

    override fun <T : Service> getService(clazz: Class<T>): T? = current().getService(clazz)

    override fun <T : UserService> getUserService(clazz: Class<T>): T? = current().getUserService(clazz)
}

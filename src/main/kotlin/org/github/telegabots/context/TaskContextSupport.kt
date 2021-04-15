package org.github.telegabots.context

import org.github.telegabots.api.Service
import org.github.telegabots.api.TaskContext
import org.github.telegabots.api.UserService

object TaskContextSupport : TaskContext {
    override fun blockId(): Long {
        TODO("Not yet implemented")
    }

    override fun pageId(): Long {
        TODO("Not yet implemented")
    }

    override fun <T : Service> getService(clazz: Class<T>): T? {
        TODO("Not yet implemented")
    }

    override fun <T : UserService> getUserService(clazz: Class<T>): T? {
        TODO("Not yet implemented")
    }
}

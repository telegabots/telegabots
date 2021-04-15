package org.github.telegabots.util

import org.github.telegabots.api.BaseTask
import org.github.telegabots.api.annotation.TaskHandler
import java.lang.reflect.Method

class TaskClassUtil {
    fun getHandlers(command: BaseTask): List<TaskHandlerInfo> {
        return command.javaClass.methods
            .mapNotNull { mapHandler(it, command) }
            .map { checkHandler(it) }
    }

    private fun mapHandler(method: Method, task: BaseTask): TaskHandlerInfo? {
        if (isTaskHandler(method)) {
            return TaskHandlerInfo(
                name = method.name,
                method = method,
                params = HandlerParamUtil.getParams(method),
                retType = method.returnType,
                task = task
            )
        }

        return null
    }

    private fun checkHandler(it: TaskHandlerInfo): TaskHandlerInfo {
        TODO("Not yet implemented")
    }

    private fun isTaskHandler(method: Method): Boolean =
        method.annotations.any { it.annotationClass == TaskHandler::class }
}

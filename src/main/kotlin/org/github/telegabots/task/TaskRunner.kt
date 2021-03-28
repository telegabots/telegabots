package org.github.telegabots.task

import org.github.telegabots.api.BaseTask
import java.util.concurrent.Callable

class TaskRunner(
    private val task: BaseTask
) : Callable<Any> {
    override fun call(): Any {
        TODO("Not yet implemented")
    }
}

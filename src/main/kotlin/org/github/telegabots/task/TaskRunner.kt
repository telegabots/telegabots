package org.github.telegabots.task

import org.github.telegabots.api.BaseTask
import java.util.concurrent.Callable

class TaskRunner(
    private val task: BaseTask
) : Callable<Any?> {
    var called = false
        private set

    override fun call(): Any? {
        //task.run()
        called = true
        return null
    }
}

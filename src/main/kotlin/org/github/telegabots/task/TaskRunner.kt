package org.github.telegabots.task

import org.github.telegabots.api.BaseTask
import java.util.concurrent.Callable
import java.util.function.BiConsumer

class TaskRunner(
    private val task: BaseTask,
    private val taskStoppedHandler: BiConsumer<BaseTask, Exception?>
) : Callable<Any?> {
    var called = false
        private set

    override fun call(): Any? {
        try {
            //task.run()
            called = true
            taskStoppedHandler.accept(task, null)
        } catch (e: java.lang.Exception) {
            called = true
            taskStoppedHandler.accept(task, e)
        }

        return null
    }
}

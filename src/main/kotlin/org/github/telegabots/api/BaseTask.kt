package org.github.telegabots.api

import org.github.telegabots.context.TaskContextSupport
import org.github.telegabots.task.DefaultTaskIdGenerator
import java.time.LocalDateTime

/**
 * Base class for task - long live operation subclassed by user
 */
abstract class BaseTask {
    protected val context: TaskContext = TaskContextSupport

    /**
     *  Identifier of the task
     *
     *  Can be used in search
     */
    open fun id(): String = DefaultTaskIdGenerator.getId(this)

    /**
     * Estimate time when task will finish
     *
     * Not null if can be calculated
     */
    open fun estimateEndTime(): LocalDateTime? = null

    /**
     * Human-friendly task description
     */
    abstract fun title(): String

    /**
     * Request to stop task
     */
    abstract fun stopAsync()

    /**
     * Progress percentage of the task. Value from 0 to 100
     */
    open fun progress(): Int? = null

    /**
     * Short description of the current operation
     */
    open fun status(): String? = null

    /**
     * Routine method
     */
    abstract fun run()
}

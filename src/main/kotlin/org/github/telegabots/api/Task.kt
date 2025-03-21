package org.github.telegabots.api

import java.time.LocalDateTime
import java.util.function.Consumer

interface Task {
    /**
     *  Identifier of the task
     *
     *  Can be used in search
     */
    fun id(): String

    /**
     * Human-friendly task description
     */
    fun title(): String

    /**
     * Returns state of the task
     */
    fun state(): TaskState

    /**
     * Runs task if one not started yet
     */
    fun start(onComplete: Consumer<TaskRunResult>? = null)

    /**
     * Async stop task
     */
    fun stop()

    /**
     * Short description of the current operation
     */
    fun status(): String?

    /**
     * Time when task was actually started
     */
    fun startedTime(): LocalDateTime?

    /**
     * Estimate time when task will finished
     *
     * Not null if can be calculated
     */
    fun estimateEndTime(): LocalDateTime?

    /**
     * Progress percentage of the task. Value from 0 to 100
     */
    fun progress(): Int?
}

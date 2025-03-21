package org.github.telegabots.api

interface TaskManager : Service {
    /**
     * Register new task
     */
    fun register(task: BaseTask): Task

    /**
     * Remove task from executing. If task is running, it will be stopped before
     */
    fun unregister(task: Task)

    /**
     * Returns all registered tasks
     */
    fun getAll(): List<Task>
}

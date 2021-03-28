package org.github.telegabots.task

import org.github.telegabots.api.BaseTask
import org.github.telegabots.api.Task
import org.github.telegabots.api.TaskManager
import java.time.LocalDateTime
import java.util.concurrent.Executors

class TaskManagerImpl() : TaskManager {
    private val executorService = Executors.newCachedThreadPool()
    private val mutableList = mutableListOf<TaskWrapper>()

    override fun run(task: BaseTask): Task {
        val runner = TaskRunner(task)
        val future = executorService.submit(runner)

        return TaskWrapper(task, LocalDateTime.now(), future)
    }

    override fun getAll(): List<Task> = mutableList.toList()
}

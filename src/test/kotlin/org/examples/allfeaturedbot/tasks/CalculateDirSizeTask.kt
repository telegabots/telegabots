package org.examples.allfeaturedbot.tasks

import org.github.telegabots.api.BaseTask
import org.github.telegabots.api.annotation.TaskHandler
import java.time.LocalDateTime

/**
 * Task calculating full size of defined dir path
 */
class CalculateDirSizeTask(path: String) : BaseTask() {
    override fun id(): String {
        TODO("Not yet implemented")
    }

    override fun title(): String {
        TODO("Not yet implemented")
    }

    override fun stopAsync() {
        TODO("Not yet implemented")
    }

    override fun status(): String? {
        TODO("Not yet implemented")
    }

    override fun estimateEndTime(): LocalDateTime? {
        TODO("Not yet implemented")
    }

    override fun progress(): Int? {
        TODO("Not yet implemented")
    }

    @TaskHandler
    override fun run() {
        TODO("Not yet implemented")
    }
}
package org.examples.testbot.commands

import org.examples.testbot.tasks.CalculateDirSizeTask
import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.annotation.InlineHandler

class CalculateDirSizeCommand : BaseCommand() {
    @InlineHandler
    fun handle(message: String, dirPath: String) {
        log.info("Calculate size of the dir: {}, message: {}", dirPath, message)

        context.getTaskManager().register(CalculateDirSizeTask(dirPath)).start()
    }
}

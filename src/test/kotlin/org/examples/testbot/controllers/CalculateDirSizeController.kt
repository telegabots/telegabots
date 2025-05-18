package org.examples.testbot.controllers

import org.examples.testbot.tasks.CalculateDirSizeTask
import org.github.telegabots.api.BaseController
import org.github.telegabots.api.annotation.InlineHandler

class CalculateDirSizeController : BaseController() {
    @InlineHandler
    fun handle(message: String, dirPath: String) {
        log.info("Calculate size of the dir: {}, message: {}", dirPath, message)

        context.getTaskManager().register(CalculateDirSizeTask(dirPath)).start()
    }
}

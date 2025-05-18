package org.github.telegabots.error

import org.github.telegabots.api.BaseController
import org.github.telegabots.api.BaseTask

/**
 * Base TelegaBots exception
 */
open class BaseException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
    constructor(message: String?, cause: Throwable?, enableSuppression: Boolean, writableStackTrace: Boolean) :
            super(message, cause, enableSuppression, writableStackTrace)
}

class ControllerInvokeException(val controller: Class<out BaseController>, cause: Throwable?) :
    BaseException("Controller invoke failed: ${controller.name}, error: ${cause?.message}", cause) {
}

class TaskInvokeException(val task: Class<out BaseTask>, cause: Throwable?) :
    BaseException("Task invoke failed: ${task.name}, error: ${cause?.message}", cause) {
}

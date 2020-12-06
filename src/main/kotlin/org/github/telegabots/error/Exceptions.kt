package org.github.telegabots.error

import org.github.telegabots.BaseCommand
import java.lang.RuntimeException

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

class CommandInvokeException(val command: Class<out BaseCommand>, cause: Throwable?) :
        BaseException("Command invoke failed: ${command.name}", cause) {
}

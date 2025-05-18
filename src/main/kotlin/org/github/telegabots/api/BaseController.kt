package org.github.telegabots.api

import org.slf4j.LoggerFactory
import org.github.telegabots.context.ControllerContextSupport

/**
 * Base class of all controllers
 */
abstract class BaseController {
    @JvmField
    protected val log = LoggerFactory.getLogger(javaClass)!!
    @JvmField
    protected val context: ControllerContext = ControllerContextSupport

    /**
     * Return true if this controller can be executed only by admin role
     */
    open fun isOnlyForAdmin(): Boolean = false

    override fun toString(): String = javaClass.simpleName

    companion object {
        const val MESSAGE_START = "/start"
    }
}

fun BaseController.isEmpty() = this is EmptyController

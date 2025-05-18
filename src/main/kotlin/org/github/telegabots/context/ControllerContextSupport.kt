package org.github.telegabots.context

import org.github.telegabots.api.*

/**
 * Supports [ControllerContext] for current executing controller
 */
object ControllerContextSupport : BaseContextSupport<ControllerContext>(), ControllerContext {
    override fun inputMessage(): InputMessage = current().inputMessage()

    override fun inputMessageId(): Int = current().inputMessageId()

    override fun currentController(): BaseController = current().currentController()

    override fun execute(): Boolean  = current().execute()

    override fun isAdmin(): Boolean = current().isAdmin()

    override fun getUser(): InputUser = current().getUser()
}

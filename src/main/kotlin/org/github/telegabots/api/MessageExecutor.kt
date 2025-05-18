package org.github.telegabots.api

interface MessageExecutor {
    fun executeTextMessage(handler: Class<out BaseController>, text: String): Boolean

    fun executeInlineMessage(handler: Class<out BaseController>, query: String): Boolean
}

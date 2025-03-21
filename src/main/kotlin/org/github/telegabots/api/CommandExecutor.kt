package org.github.telegabots.api

interface CommandExecutor {
    fun executeTextCommand(handler: Class<out BaseCommand>, text: String): Boolean

    fun executeInlineCommand(handler: Class<out BaseCommand>, query: String): Boolean
}

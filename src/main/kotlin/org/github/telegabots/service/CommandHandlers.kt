package org.github.telegabots.service

import org.slf4j.LoggerFactory
import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.CommandInterceptor
import org.github.telegabots.util.CommandClassUtil
import java.util.concurrent.ConcurrentHashMap

/**
 * Creating and thread-safe storing of CommandHandler
 */
class CommandHandlers(private val commandInterceptor: CommandInterceptor = CommandInterceptor.Empty) {
    private val log = LoggerFactory.getLogger(CommandHandlers::class.java)
    private val commandHandlers = ConcurrentHashMap<String, CommandHandler>()

    fun getCommandHandler(clazz: Class<out BaseCommand>): CommandHandler {
        return getCommandHandler(clazz.name)
    }

    fun getCommandHandler(handler: String): CommandHandler {
        return commandHandlers.getOrPut(handler) {
            return createHandler(handler)
        }
    }

    private fun createHandler(handler: String): CommandHandler {
        try {
            val commandClass = this.javaClass.classLoader.loadClass(handler)
            val command = commandClass.newInstance() as BaseCommand
            val handlers = CommandClassUtil.getHandlers(command)

            return CommandHandler(command = command, handlers = handlers, commandInterceptor = commandInterceptor)
        } catch (e: ClassNotFoundException) {
            log.error("Handler not found: {}", handler, e)
            throw e
        } catch (e: Throwable) {
            log.error("Handler class load failed: {}", handler, e)
            throw e
        }
    }
}

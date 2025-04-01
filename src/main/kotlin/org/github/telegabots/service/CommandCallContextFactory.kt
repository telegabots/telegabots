package org.github.telegabots.service

import org.github.telegabots.api.*
import org.github.telegabots.task.TaskManagerFactory
import org.slf4j.LoggerFactory

/**
 * Service of creating CommandCallContext by user input
 *
 * TODO: rewrite/refactor this class
 */
internal class CommandCallContextFactory(
    private val serviceProvider: ServiceProvider,
    private val rootCommand: Class<out BaseCommand>
) {
    private val taskManagerFactory = serviceProvider.getService(TaskManagerFactory::class.java)
    private val messageSender = serviceProvider.getService(MessageSender::class.java)
    private val commandHandlers = serviceProvider.getService(CommandHandlers::class.java)

    init {
        val rootHandler = commandHandlers.getCommandHandler(rootCommand)

        check(rootHandler.canHandle(MessageType.Text)) { "Root command (${rootCommand.name}) have to implement text handler. Annotate method with @TextHandler" }

        log.info("CommandCallContextFactory created")
    }

    fun get(input: InputMessage): CommandCallContext = CommandCallContextUserFactory(
        input,
        messageSender,
        serviceProvider,
        commandHandlers,
        taskManagerFactory,
        rootCommand
    ).get()


    private companion object {
        val log = LoggerFactory.getLogger(CommandCallContextFactory::class.java)!!
    }
}

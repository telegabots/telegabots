package org.github.telegabots.service

import org.github.telegabots.api.*
import org.github.telegabots.state.UsersStatesManager
import org.github.telegabots.task.TaskManagerFactory
import org.slf4j.LoggerFactory

/**
 * Service of creating CommandCallContext by user input
 */
class CommandCallContextFactory(
    private val messageSender: MessageSender,
    private val serviceProvider: ServiceProvider,
    private val commandHandlers: CommandHandlers,
    private val usersStatesManager: UsersStatesManager,
    private val rootCommand: Class<out BaseCommand>
) {
    private val taskManagerFactory = TaskManagerFactory(serviceProvider)

    init {
        val rootHandler = commandHandlers.getCommandHandler(rootCommand)

        check(rootHandler.canHandle(MessageType.Text)) { "Root command (${rootCommand.name}) have to implement text handler. Annotate method with @TextHandler" }
    }

    fun get(input: InputMessage): CommandCallContext = getUserCommandCallContextFactory(input).get()

    private fun getUserCommandCallContextFactory(input: InputMessage): CommandCallContextUserFactory {
        return CommandCallContextUserFactory(
            input,
            messageSender,
            serviceProvider,
            commandHandlers,
            usersStatesManager.get(input.userId),
            taskManagerFactory,
            rootCommand
        )
    }

    private companion object {
        val log = LoggerFactory.getLogger(CommandCallContextFactory::class.java)!!
    }
}

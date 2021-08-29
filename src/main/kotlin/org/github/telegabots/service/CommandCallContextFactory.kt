package org.github.telegabots.service

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.InputMessage
import org.github.telegabots.api.MessageSender
import org.github.telegabots.api.MessageType
import org.github.telegabots.api.ServiceProvider
import org.github.telegabots.api.UserLocalizationFactory
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
    private val userLocalizationFactory: UserLocalizationFactory,
    private val rootCommand: Class<out BaseCommand>
) {
    private val taskManagerFactory = TaskManagerFactory(serviceProvider)

    init {
        val rootHandler = commandHandlers.getCommandHandler(rootCommand)

        check(rootHandler.canHandle(MessageType.Text)) { "Root command (${rootCommand.name}) have to implement text handler. Annotate method with @TextHandler" }
    }

    fun get(input: InputMessage): CommandCallContext = getUserCommandCallContextFactory(input).get()

    private fun getUserCommandCallContextFactory(input: InputMessage): CommandCallContextUserFactory =
        CommandCallContextUserFactory(
            input,
            messageSender,
            serviceProvider,
            commandHandlers,
            usersStatesManager.get(input.userId),
            userLocalizationFactory.getProvider(input.userId),
            taskManagerFactory,
            rootCommand
        )

    private companion object {
        val log = LoggerFactory.getLogger(CommandCallContextFactory::class.java)!!
    }
}

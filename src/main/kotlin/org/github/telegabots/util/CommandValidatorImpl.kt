package org.github.telegabots.util

import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.CommandValidator
import org.github.telegabots.service.CommandHandlers
import org.reflections.Reflections
import org.slf4j.LoggerFactory

class CommandValidatorImpl(private val commandHandlers: CommandHandlers) : CommandValidator {
    private val log = LoggerFactory.getLogger(javaClass)!!

    /**
     * Validates specified command classes
     */
    override fun validate(vararg classes: Class<out BaseCommand>) {
        classes.forEach { cmdClass ->
            log.debug("Validating command: {}", cmdClass.name)
            commandHandlers.getCommandHandler(cmdClass)
        }
    }

    /**
     * Finds all command classes by packagePrefix and validates them
     */
    override fun validateAll(packagePrefix: String) {
        val reflections = Reflections(packagePrefix)
        val allCommands = reflections.getSubTypesOf(BaseCommand::class.java)

        validate(*allCommands.toTypedArray())
    }
}

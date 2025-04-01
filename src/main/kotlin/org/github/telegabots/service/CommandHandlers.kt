package org.github.telegabots.service

import org.github.telegabots.api.*
import org.github.telegabots.util.CommandClassUtil
import org.slf4j.LoggerFactory
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

/**
 * Creating and thread-safe storing of CommandHandler
 */
internal class CommandHandlers(
    private val serviceProvider: ServiceProvider
) : Service {
    private val log = LoggerFactory.getLogger(CommandHandlers::class.java)
    private val commandHandlers = ConcurrentHashMap<String, CommandHandler>()

    fun getCommandHandler(clazz: Class<out BaseCommand>): CommandHandler {
        return getCommandHandler(clazz.name)
    }

    fun getCommandHandler(handler: String): CommandHandler {
        return commandHandlers.getOrPut(handler) {
            return createHandler(handler, false)
        }
    }

    fun validate(clazz: Class<out BaseCommand>) {
        createHandler(clazz.name, true)
    }

    private fun createHandler(handler: String, onlyValidate: Boolean): CommandHandler {
        try {
            // Command is stateless and can be created once
            val commandClass = this.javaClass.classLoader.loadClass(handler) as? Class<BaseCommand>
                ?: throw ClassNotFoundException("Command class not command: $handler")
            check(classCanBeInstantiated(commandClass)) { "Command class cannot be created : $handler" }
            // Command class can contain multiple Service references in constructor
            val command = createBaseCommandInstance(commandClass, onlyValidate)
            if (command != null) {
                val handlers = CommandClassUtil.getHandlers(command)
                return CommandHandler(command = command, handlers = handlers)
            }

            // This is only validation
            CommandClassUtil.checkHandlers(commandClass)
            return CommandHandler(command = EmptyCommand.INSTANCE, handlers = emptyList())
        } catch (e: ClassNotFoundException) {
            log.error("Handler not found: {}", handler, e)
            throw e
        } catch (e: InstantiationException) {
            if (e.cause is NoSuchMethodException)
                log.error("Default constructor not found: {}", handler, e)
            else
                log.error("Handler class load failed: {}", handler, e)
            throw e
        } catch (e: Throwable) {
            log.error("Handler class load failed: {}", handler, e)
            throw e
        }
    }

    private fun createBaseCommandInstance(commandClass: Class<BaseCommand>, onlyValidate: Boolean): BaseCommand? {
        // Find constructor with max parameters
        val constructor = commandClass.constructors.maxBy { it.parameterCount }
        check(constructor != null) { "Command class should have at least one constructor: ${commandClass.name}" }
        // Create instances of all constructor parameters
        val allServices = constructor.parameters
            .map { parameter -> parameter.type }
            .map { checkConstructorParameter(it, commandClass.name) }

        if (!onlyValidate) {
            val args = allServices
                .map { service -> serviceProvider.getService(service) }
                .toTypedArray()
            return (if (args.isNotEmpty()) constructor.newInstance(*args) else constructor.newInstance()) as BaseCommand
        }

        return null
    }

    private fun checkConstructorParameter(serviceType: Class<*>, commandClass: String): Class<Service> {
        if (UserService::class.java.isAssignableFrom(serviceType)) {
            error("UserService is not allowed in command constructor of $commandClass. Only Service")
        }
        if (Service::class.java.isAssignableFrom(serviceType)) {
            return serviceType as Class<Service>
        }
        error("Argument type not implements Service: $serviceType. Command: $commandClass")
    }

    private fun classCanBeInstantiated(commandClass: Class<BaseCommand>): Boolean {
        // return true if class is not abstract and not interface
        val modifiers = commandClass.modifiers
        // TODO: improve this check
        return !Modifier.isAbstract(modifiers) && !Modifier.isInterface(modifiers)
                && !commandClass.isAnnotation && !commandClass.isEnum
    }
}

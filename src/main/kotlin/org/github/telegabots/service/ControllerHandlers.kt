package org.github.telegabots.service

import org.github.telegabots.api.*
import org.github.telegabots.util.ControllerClassUtil
import org.slf4j.LoggerFactory
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

/**
 * Creates and stores [ControllerHandler]s
 */
internal class ControllerHandlers(
    private val serviceProvider: ServiceProvider
) : Service {
    private val controllerHandlers = ConcurrentHashMap<String, ControllerHandler>()

    fun getControllerHandler(clazz: Class<out BaseController>): ControllerHandler {
        return getControllerHandler(clazz.name)
    }

    fun getControllerHandler(handler: String): ControllerHandler {
        return controllerHandlers.getOrPut(handler) {
            return createHandler(handler, false)
        }
    }

    fun validate(clazz: Class<out BaseController>) {
        createHandler(clazz.name, true)
    }

    private fun createHandler(handler: String, onlyValidate: Boolean): ControllerHandler {
        try {
            // Controller is stateless and can be created once
            val controllerClass = this.javaClass.classLoader.loadClass(handler) as? Class<BaseController>
                ?: throw ClassNotFoundException("Controller class not found: $handler")
            check(classCanBeInstantiated(controllerClass)) { "Controller class cannot be created: $handler" }
            // Controller class can contain multiple Service references in constructor
            val controller = createBaseControllerInstance(controllerClass, onlyValidate)
            if (controller != null) {
                val handlers = ControllerClassUtil.getHandlers(controller)
                return ControllerHandler(controller = controller, handlers = handlers)
            }

            // This is only validation
            ControllerClassUtil.checkHandlers(controllerClass)
            return ControllerHandler(controller = EmptyController.INSTANCE, handlers = emptyList())
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

    private fun createBaseControllerInstance(
        controllerClass: Class<BaseController>,
        onlyValidate: Boolean
    ): BaseController? {
        // Find constructor with max parameters
        val constructor = controllerClass.constructors.maxBy { it.parameterCount }
        check(constructor != null) { "Controller class should have at least one constructor: ${controllerClass.name}" }
        // Create instances of all constructor parameters
        val allServices = constructor.parameters
            .map { parameter -> parameter.type }
            .map { checkConstructorParameter(it, controllerClass.name) }

        if (!onlyValidate) {
            val args = allServices
                .map { service -> serviceProvider.getService(service) }
                .toTypedArray()
            return (if (args.isNotEmpty()) constructor.newInstance(*args) else constructor.newInstance()) as BaseController
        }

        return null
    }

    private fun checkConstructorParameter(serviceType: Class<*>, controllerClass: String): Class<Service> {
        if (UserService::class.java.isAssignableFrom(serviceType)) {
            error("UserService is not allowed in controller constructor of $controllerClass. Only Service")
        }
        if (Service::class.java.isAssignableFrom(serviceType)) {
            return serviceType as Class<Service>
        }
        error("Argument type not implements Service: $serviceType. Controller: $controllerClass")
    }

    private fun classCanBeInstantiated(controllerClass: Class<BaseController>): Boolean {
        // return true if class is not abstract and not interface
        val modifiers = controllerClass.modifiers
        // TODO: improve this check
        return !Modifier.isAbstract(modifiers) && !Modifier.isInterface(modifiers)
                && !controllerClass.isAnnotation && !controllerClass.isEnum
    }

    private companion object {
        val log = LoggerFactory.getLogger(ControllerHandlers::class.java)!!
    }
}

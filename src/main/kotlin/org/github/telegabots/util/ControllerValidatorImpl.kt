package org.github.telegabots.util

import org.github.telegabots.api.BaseController
import org.github.telegabots.api.ControllerValidator
import org.github.telegabots.service.ControllerHandlers
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import java.lang.reflect.Modifier

internal class ControllerValidatorImpl(private val controllerHandlers: ControllerHandlers) : ControllerValidator {
    /**
     * Validates specified [BaseController] classes
     */
    override fun validate(vararg classes: Class<out BaseController>) {
        classes.forEach { controllerClass ->
            log.debug("Validating controller: {}", controllerClass.name)
            controllerHandlers.validate(controllerClass)
        }
    }

    /**
     * Finds all controller classes by packagePrefix and validates them
     */
    override fun validateAll(packagePrefix: String) {
        val reflections = Reflections(packagePrefix)
        val allControllers = reflections.getSubTypesOf(BaseController::class.java)
            .filter { !Modifier.isAbstract(it.modifiers) }

        check(allControllers.isNotEmpty()) { "No controllers found in package: $packagePrefix" }

        validate(*allControllers.toTypedArray())
    }

    private companion object {
        val log = LoggerFactory.getLogger(ControllerValidatorImpl::class.java)!!
    }
}

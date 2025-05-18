package org.github.telegabots.api

/**
 * [BaseController] class validator
 */
interface ControllerValidator : Service {
    /**
     * Validates specified controller classes
     */
    fun validate(vararg classes: Class<out BaseController>)

    /**
     * Finds all controller classes by packagePrefix and validates them
     */
    fun validateAll(packagePrefix: String)
}

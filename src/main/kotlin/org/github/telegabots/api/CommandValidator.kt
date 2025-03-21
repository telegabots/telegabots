package org.github.telegabots.api

/**
 * Command class validator
 */
interface CommandValidator : Service {
    /**
     * Validates specified command classes
     */
    fun validate(vararg classes: Class<out BaseCommand>)

    /**
     * Finds all command classes by packagePrefix and validates them
     */
    fun validateAll(packagePrefix: String)
}

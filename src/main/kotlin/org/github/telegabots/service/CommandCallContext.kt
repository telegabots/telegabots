package org.github.telegabots.service

/**
 * Command call context
 */
interface CommandCallContext {
    fun execute(): Boolean
}

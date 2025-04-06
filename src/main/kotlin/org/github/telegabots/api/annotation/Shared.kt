package org.github.telegabots.api.annotation

/**
 * Mark method parameter for shared state for each command of specified message
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Shared(val name: String = "")

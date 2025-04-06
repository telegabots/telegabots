package org.github.telegabots.api.annotation

/**
 * Default. Mark method parameter for local state of a command. Used when state must be named
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Local(val name: String = "")

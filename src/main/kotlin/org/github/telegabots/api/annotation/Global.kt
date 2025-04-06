package org.github.telegabots.api.annotation

/**
 * Mark method parameter for global state shared between all users
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Global(val name: String = "")

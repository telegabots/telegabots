package org.github.telegabots.api.annotation

/**
 * Global state shared between all users
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Global(val name: String = "")

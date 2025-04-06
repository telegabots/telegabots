package org.github.telegabots.api.annotation

/**
 * Mark method parameter for user state shared between all messages
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class User(val name: String = "")

package org.github.telegabots.api.annotation

/**
 * State related to current user. Shared between all blocks of the user.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class User(val name: String = "")

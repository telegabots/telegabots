package org.github.telegabots.api.annotation

/**
 * Default. State related to current page.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Local(val name: String = "")

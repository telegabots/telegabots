package org.github.telegabots.api.annotation

/**
 * Mark method if one must be called when task started
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class TaskHandler

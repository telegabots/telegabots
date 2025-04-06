package org.github.telegabots.api.annotation

/**
 * Mark method if one must be called when inline (callback) query come
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class InlineHandler

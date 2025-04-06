package org.github.telegabots.api.annotation

/**
 * Mark method if one must be called when simple text message come
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class TextHandler

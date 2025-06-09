package org.github.telegabots.api.annotation

/**
 * Annotation to mark method as error handler. First parameter of the method should be of type [Throwable].
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ErrorHandler

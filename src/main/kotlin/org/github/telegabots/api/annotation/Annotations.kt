package org.github.telegabots.api.annotation

/**
 * Mark method if one must be called when simple text message come
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class CommandHandler

/**
 * Mark method if one must be called when callback query come
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class CallbackHandler

/**
 * Default. Mark method parameter for local state of a command. Used when state must be named
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Local(val name: String = "")

/**
 * Mark method parameter for shared state for each command of specified message
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Shared(val name: String = "")

/**
 * Mark method parameter for user state shared between all messages
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class User(val name: String = "")

/**
 * Mark method parameter for global state shared between all users
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Global(val name: String = "")

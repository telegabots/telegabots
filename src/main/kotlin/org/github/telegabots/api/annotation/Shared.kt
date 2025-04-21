package org.github.telegabots.api.annotation

/**
 * State related to current block. Shared between all pages of the block.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Shared(val name: String = "")

package org.github.telegabots.api.annotation

import org.github.telegabots.api.entity.BaseEntity

/**
 * Mark field or property of an [BaseEntity] as unique.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Unique1()

package org.github.telegabots.api.annotation

import org.github.telegabots.api.entity.BaseEntity

/**
 * Mark field or property of an [BaseEntity] with index.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Index1()

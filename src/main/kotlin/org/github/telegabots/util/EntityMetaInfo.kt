package org.github.telegabots.util

import org.github.telegabots.api.annotation.Index1
import org.github.telegabots.api.annotation.Unique1
import org.github.telegabots.api.entity.BaseEntity
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.jvm.kotlinProperty

/**
 * Information about entity fields annotated with [Unique1] and [Index1].
 */
internal data class EntityMetaInfo(
    private val unique1: FieldInfo? = null,
    private val index1: FieldInfo? = null
) {

    fun hasUnique1(): Boolean = unique1 != null

    fun hasIndex1(): Boolean = index1 != null

    fun getUnique1(obj: BaseEntity): String? = unique1?.getValue(obj) as String?

    fun getIndex1(obj: BaseEntity): Long? = index1?.getValue(obj) as Long?

    fun setUnique1(obj: BaseEntity, value: String?) {
        unique1?.setValue(obj, value)
    }

    fun setIndex1(obj: BaseEntity, value: Long?) {
        index1?.setValue(obj, value)
    }

    companion object {
        val Empty = EntityMetaInfo()

        private val cache: MutableMap<Class<out BaseEntity>, EntityMetaInfo> = ConcurrentHashMap()

        fun of(entityClass: Class<out BaseEntity>): EntityMetaInfo {
            return cache.computeIfAbsent(entityClass) {
                parseInternal(entityClass)
            }
        }

        private fun parseInternal(entityClass: Class<out BaseEntity>): EntityMetaInfo {
            val fields = entityClass.declaredFields.mapNotNull { field -> mapField(field, entityClass) }

            if (fields.isNotEmpty()) {
                val unique1Fields = fields.filter { it.annotation.annotationClass == Unique1::class }

                if (unique1Fields.size > 1) {
                    error("Only one field can be annotated with @Unique1, found in ${entityClass.name}: ${unique1Fields.joinToString { it.field.name }}")
                }

                val index1Fields = fields.filter { it.annotation.annotationClass == Index1::class }

                if (index1Fields.size > 1) {
                    error("Only one field can be annotated with @Index1, found in ${entityClass.name}: ${index1Fields.joinToString { it.field.name }}")
                }

                return EntityMetaInfo(
                    unique1 = unique1Fields.firstOrNull(),
                    index1 = index1Fields.firstOrNull()
                )
            }

            return Empty
        }

        private fun mapField(field: Field, clazz: Class<out BaseEntity>): FieldInfo? {
            val unique1 = sequenceOf(field.annotations.toList(), field.kotlinProperty?.annotations)
                .filterNotNull()
                .flatMap { it }
                .filter { it.annotationClass == Unique1::class }
                .map { it as Unique1 }
                .firstOrNull()

            if (unique1 != null && field.type != String::class.java) {
                error("@Unique1 can only be used with String type, field: ${clazz.name}.${field.name}")
            }

            val index1 = sequenceOf(field.annotations.toList(), field.kotlinProperty?.annotations)
                .filterNotNull()
                .flatMap { it }
                .filter { it.annotationClass == Index1::class }
                .map { it as Index1 }
                .firstOrNull()

            if (unique1 != null && index1 != null) {
                error("@Unique1 and @Index1 can not be used together, field: ${clazz.name}.${field.name}")
            }

            if (index1 != null && field.type != Long::class.java && field.type != java.lang.Long::class.java) {
                error("@Index1 can only be used with Long type, field: ${clazz.name}.${field.name}")
            }

            if (unique1 != null) {
                return FieldInfo(field, unique1)
            }
            if (index1 != null) {
                return FieldInfo(field, index1)
            }
            return null
        }
    }
}

internal data class FieldInfo(val field: Field, val annotation: Annotation) {
    fun getValue(any: BaseEntity): Any? {
       field.isAccessible = true
        return field.get(any)
    }

    fun setValue(any: BaseEntity, value: Any?) {
        field.isAccessible = true
        field.set(any, value)
    }
}

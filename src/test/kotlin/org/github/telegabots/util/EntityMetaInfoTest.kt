package org.github.telegabots.util

import org.github.telegabots.api.annotation.Index1
import org.github.telegabots.api.annotation.Unique1
import org.github.telegabots.api.entity.BaseEntity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests [EntityMetaInfo]
 */
class EntityMetaInfoTest {
    @Test
    fun testEmptyEntity() {
        val metaInfo = EntityMetaInfo.of(TestEmptyEntity::class.java)

        assertFalse(metaInfo.hasIndex1())
        assertFalse(metaInfo.hasUnique1())

        val entity = TestEmptyEntity()
        assertNull(metaInfo.getIndex1(entity))
        assertNull(metaInfo.getUnique1(entity))

        // Check that the entity info is cached
        assertSame(metaInfo, EntityMetaInfo.of(TestEmptyEntity::class.java))
    }

    @Test
    fun testEntityWithAnnotatedFields() {
        val metaInfo = EntityMetaInfo.of(TestEntity3::class.java)

        assertTrue(metaInfo.hasIndex1())
        assertTrue(metaInfo.hasUnique1())

        val entity = TestEntity3(123L, "test", 456L)
        assertEquals("test", metaInfo.getUnique1(entity))
        assertEquals(456L, metaInfo.getIndex1(entity))

        metaInfo.setUnique1(entity, "new value")
        assertEquals("new value", metaInfo.getUnique1(entity))
        assertEquals("new value", entity.name)

        metaInfo.setIndex1(entity, 789L)
        assertEquals(789L, metaInfo.getIndex1(entity))
        assertEquals(789L, entity.parentId)

        // Check that the entity info is cached
        assertSame(metaInfo, EntityMetaInfo.of(TestEntity3::class.java))
    }

    @Test
    fun testEntityWithIncorrectUnique1() {
        val ex = assertThrowsExactly(IllegalStateException::class.java) {
            EntityMetaInfo.of(TestEntityIncorrect::class.java)
        }
        val expected =
            "Only one field can be annotated with @Unique1, found in org.github.telegabots.util.TestEntityIncorrect: name1, name2"
        assertEquals(expected, ex.message)
    }

    @Test
    fun testEntityWithIncorrectIndex1() {
        val ex = assertThrowsExactly(IllegalStateException::class.java) {
            EntityMetaInfo.of(TestEntityIncorrect2::class.java)
        }
        val expected =
            "Only one field can be annotated with @Index1, found in org.github.telegabots.util.TestEntityIncorrect2: val1, val2"
        assertEquals(expected, ex.message)
    }

    @Test
    fun testEntityWithIncorrectTypeWithUnique1() {
        val ex = assertThrowsExactly(IllegalStateException::class.java) {
            EntityMetaInfo.of(TestEntityIncorrect4::class.java)
        }
        val expected =
            "@Unique1 can only be used with String type, field: org.github.telegabots.util.TestEntityIncorrect4.name"
        assertEquals(expected, ex.message)
    }

    @Test
    fun testEntityWithIncorrectTypeWithIndex1() {
        val ex = assertThrowsExactly(IllegalStateException::class.java) {
            EntityMetaInfo.of(TestEntityIncorrect3::class.java)
        }
        val expected =
            "@Index1 can only be used with Long type, field: org.github.telegabots.util.TestEntityIncorrect3.name"
        assertEquals(expected, ex.message)
    }
}

internal class TestEntity3(
    id: Long?,
    @Unique1
    val name: String, // even val property can be changed! wow!
    @Index1
    val parentId: Long
) : BaseEntity(id)

internal class TestEmptyEntity : BaseEntity()

data class TestEntityIncorrect(@Unique1 val name1: String, @Unique1 val name2: String) : BaseEntity()

data class TestEntityIncorrect2(@Index1 val val1: Long, @Index1 val val2: Long) : BaseEntity()

data class TestEntityIncorrect3(@Index1 val name: String) : BaseEntity()

data class TestEntityIncorrect4(@Unique1 val name: Long) : BaseEntity()

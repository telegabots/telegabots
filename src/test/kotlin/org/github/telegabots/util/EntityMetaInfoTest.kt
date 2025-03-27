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
}

internal class TestEntity3(
    private var id: Long?,
    @Unique1
    val name: String, // even val property can be changed! wow!
    @Index1
    val parentId: Long
) : BaseEntity() {
    override fun getId(): Long? = id

    override fun setId(id: Long?) {
        this.id = id
    }
}

internal class TestEmptyEntity() : BaseEntity() {
    override fun getId(): Long? = null

    override fun setId(id: Long?) {
        // do nothing
    }
}

package org.github.telegabots.api

import org.github.telegabots.api.entity.BaseEntity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * Test any implementation of [EntityRepository]
 */
abstract class EntityRepositoryTest {
    protected val random = Random.Default

    abstract fun <T : BaseEntity> getRepository(
        clazz: Class<T>,
        userId: Long,
        repository: EntityRepository<*>? = null
    ): EntityRepository<T>

    @Test
    fun testSave() {
        val repository = getRepository(TestEntity::class.java, 123L)
        assertEquals(0, repository.count())

        val entity1 = createEntity()
        val newEntity1 = repository.save(entity1)

        assertEquals(1, repository.count())
        assertNotNull(newEntity1.getId())
        assertSame(entity1, newEntity1, "Method save should return the same entity with id")
        val storedEntity1 = repository.findById(newEntity1.getId()!!)
        assertEquals(entity1, storedEntity1)

        // Check the same entity but without id, new entity should be created
        val entity2 = entity1.copy(id = null)

        val newEntity2 = repository.save(entity2)

        assertEquals(2, repository.count())
        assertNotNull(newEntity2.getId())
        assertSame(entity2, newEntity2)
        assertNotSame(entity1, entity2)
        assertNotEquals(entity1, entity2)
        val storedEntity2 = repository.findById(entity2.getId()!!)
        assertEquals(entity2, storedEntity2)
    }

    @Test
    fun testSave_WhenUpdate() {
        val repository = getRepository(TestEntity::class.java, 123L)
        val entity1 = repository.save(createEntity())

        val entity2 = repository.save(entity1.copy(weight = 108.0, name = "new name"))
        assertEquals(entity1.getId(), entity2.getId())

        val loadedEntity = repository.findById(entity2.getId()!!)
        assertEquals(entity2, loadedEntity)
        assertNotEquals(entity1, loadedEntity)
        assertEquals(1, repository.count())
    }

    @Test
    fun testSave_DifferentTypes() {
        val repository1 = getRepository(TestEntity::class.java, 123L)
        val repository2 = getRepository(TestEntity2::class.java, 123L, repository1)

        assertEquals(0, repository1.count())
        assertEquals(0, repository2.count())

        val entity1 = repository1.save(createEntity())
        val entity2 = repository2.save(TestEntity2(name2 = "Test2"))
        val entity3 = repository1.save(createEntity())
        assertNotEquals(entity1.getId(), entity2.getId())
        assertNull(repository1.findById(entity2.getId()!!), "Entity should not be found by id of another type")
        assertNull(repository2.findById(entity1.getId()!!), "Entity should not be found by id of another type")

        // check findAll
        assertEquals(listOf(entity1, entity3), repository1.findAll())
        assertEquals(listOf(entity2), repository2.findAll())
        assertEquals(2, repository1.count())
        assertEquals(1, repository2.count())
    }

    @Test
    fun testDelete() {
        val repository = getRepository(TestEntity::class.java, 123L)
        val entity1 = repository.save(createEntity())

        val entityId: Long = entity1.getId()!!
        assertTrue(repository.existsById(entityId))
        assertTrue(repository.delete(entityId))
        assertFalse(repository.existsById(entityId))
        assertNull(repository.findById(entityId))
        assertFalse(repository.delete(entityId))
    }

    private fun createEntity() = TestEntity(
        name = "Test" + random.nextLong(),
        date = LocalDateTime.now(),
        volume = if (random.nextBoolean()) random.nextLong() else null,
        level = random.nextInt(),
        active = random.nextBoolean(),
        weight = if (random.nextBoolean()) random.nextDouble() else null,
        height = random.nextFloat(),
        age = 39,
        index = 127
    )
}

data class TestEntity(
    private var id: Long? = null,
    val name: String,
    val date: LocalDateTime,
    val volume: Long?,
    val level: Int,
    val active: Boolean,
    val weight: Double?,
    val height: Float,
    val age: Short,
    val index: Byte
) : BaseEntity() {
    override fun getId(): Long? = id

    override fun setId(id: Long?) {
        this.id = id
    }
}

class TestEntity2(
    private var id: Long? = null,
    val name2: String
) : BaseEntity() {
    override fun getId(): Long? = id

    override fun setId(id: Long?) {
        this.id = id
    }

    override fun toString(): String {
        return "TestEntity2(id=$id, name2='$name2')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        other as TestEntity2

        if (id != other.id) return false
        if (name2 != other.name2) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + name2.hashCode()
        return result
    }
}

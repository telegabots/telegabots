package org.github.telegabots.api

import org.github.telegabots.api.annotation.Index1
import org.github.telegabots.api.annotation.Unique1
import org.github.telegabots.api.entity.BaseEntity
import org.github.telegabots.util.TestEntityIncorrect
import org.jooq.exception.IntegrityConstraintViolationException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
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
        val entity2 = entity1.copy().apply { setId(null) }

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

        val entity2 = repository.save(entity1.copy(weight = 108.0, name = "new name").apply { setId(entity1.getId()) })
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

    @Test
    fun testFindPage() {
        val repository = getRepository(TestEntity::class.java, 123L)
        val entities = (1..101).map { createEntity() }
        repository.saveAll(entities)

        val page1 = repository.findPage(0, 20)
        assertEquals(20, page1.getContent().size)
        assertEquals(0, page1.getPage())
        assertEquals(20, page1.getSize())
        assertEquals(6, page1.getTotalPages())
        assertEquals(101, page1.getTotalElements())

        val page2 = repository.findPage(5, 20)
        assertEquals(1, page2.getContent().size)
        assertEquals(5, page2.getPage())
        assertEquals(20, page2.getSize())
        assertEquals(6, page2.getTotalPages())
        assertEquals(101, page2.getTotalElements())

        val page3 = repository.findPage(1, 50)
        assertEquals(50, page3.getContent().size)
        assertEquals(1, page3.getPage())
        assertEquals(50, page3.getSize())
        assertEquals(3, page3.getTotalPages())
        assertEquals(101, page3.getTotalElements())

        val emptyPage = repository.findPage(6, 20)
        assertEquals(emptyList<BaseEntity>(), emptyPage.getContent())
        assertEquals(6, emptyPage.getPage())
        assertEquals(20, emptyPage.getSize())
        assertEquals(6, emptyPage.getTotalPages())
        assertEquals(101, emptyPage.getTotalElements())
        assertFalse(emptyPage.hasContent())
    }

    @Test
    fun testFindByFilter() {
        val repository = getRepository(TestEntity::class.java, 123L)
        val entities = (1..112).map { createEntity(it) }
        repository.saveAll(entities)

        val filtered1 = repository.findByFilter(9) { it.name.endsWith("3") }
        assertEquals(9, filtered1.size)
        assertEquals("Test3", filtered1[0].name)
        assertEquals("Test83", filtered1[8].name)

        val filtered2 = repository.findByFilter(15) { it.name.endsWith("3") }
        assertEquals(11, filtered2.size)
        assertEquals("Test3", filtered2[0].name)
        assertEquals("Test103", filtered2[10].name)
    }

    @Test
    fun testSave_WithUnique1Constraint() {
        val repository = getRepository(TestEntity2::class.java, 123L)
        val entity1 = TestEntity2(name2 = "Test1")
        val entity2 = TestEntity2(name2 = "Test2")
        val entity3 = TestEntity2(name2 = "Test1")

        assertEquals(entity1, repository.save(entity1))
        assertEquals(entity2, repository.save(entity2))

        val ex = assertThrowsExactly(IntegrityConstraintViolationException::class.java) { repository.save(entity3) }
        assertTrue(ex.message!!.contains("A UNIQUE constraint failed")) { "Message: ${ex.message}" }

        assertEquals(2, repository.count())

        // but update should work
        entity3.setId(entity1.getId())
        repository.save(entity3)

        assertEquals(2, repository.count())
    }

    @Test
    fun testSave_WithIndex1() {
        val repository = getRepository(TestEntity2::class.java, 123L)
        val entities = (1..11).map { index ->
            TestEntity2(
                name2 = "Test$index",
                category = if (index % 2 == 0) CAT_42 else CAT_MAX
            )
        }
        repository.saveAll(entities)

        assertEquals(11, repository.count())

        val query1 = repository.query().whereIndex1(CAT_42) // category == CAT_42
        assertEquals(5, query1.count())
        val cat42List = query1.findAll().map { p -> p.category }.distinct()
        assertEquals(1, cat42List.size)
        assertEquals(CAT_42, cat42List[0])

        val query2 = repository.query().whereIndex1(CAT_MAX) // category == CAT_MAX
        assertEquals(6, query2.count())
        val catMaxList = query2.findAll().map { p -> p.category }.distinct()
        assertEquals(1, catMaxList.size)
        assertEquals(CAT_MAX, catMaxList[0])
    }

    @Test
    fun testSave_FailWhenTwoUnique1() {
        val ex = assertThrowsExactly(IllegalStateException::class.java) {
            getRepository(
                TestEntityIncorrect::class.java,
                123L
            )
        }
        val expected =
            "Only one field can be annotated with @Unique1, found in org.github.telegabots.util.TestEntityIncorrect: name1, name2"
        assertEquals(expected, ex.message)
    }

    @Test
    @Disabled("Manual test")
    fun testQueryFindPage() {
        val repository = getRepository(TestEntity2::class.java, 123L)
        val entities = (1..10_000).map { index ->
            TestEntity2(
                name2 = "Test$index",
                category = if (index % 2 == 0) CAT_42 else CAT_MAX
            )
        }
        log.info("Saving ${entities.size} entities...")
        val startTime = System.currentTimeMillis()
        repository.saveAll(entities)
        log.info("Saved ${entities.size} entities in ${System.currentTimeMillis() - startTime} ms")
        val startTime2 = System.currentTimeMillis()
        repository.save(TestEntity2(name2 = "Test10001", category = CAT_MIN))
        log.info("Saved 1 entity in ${System.currentTimeMillis() - startTime2} ms")
        assertEquals(10_001, repository.count())

        val startTime3 = System.currentTimeMillis()
        val minList = repository.query().whereIndex1(CAT_MIN).findAll()
        log.info("Found ${minList.size} entities in ${System.currentTimeMillis() - startTime3} ms")
        assertEquals(1, minList.size)
        assertEquals(CAT_MIN, minList[0].category)
    }

    private fun createEntity(index: Int = random.nextInt()) = TestEntity(
        name = "Test$index",
        date = LocalDateTime.now(),
        volume = if (random.nextBoolean()) random.nextLong() else null,
        level = random.nextInt(),
        active = random.nextBoolean(),
        weight = if (random.nextBoolean()) random.nextDouble() else null,
        height = random.nextFloat(),
        age = 39,
        index = 127
    )

    private companion object {
        const val CAT_42 = 42L
        const val CAT_MAX = Long.MAX_VALUE
        const val CAT_MIN = Long.MIN_VALUE
        val log = LoggerFactory.getLogger(EntityRepositoryTest::class.java)!!
    }
}

data class TestEntity(
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
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TestEntity

        if (getId() != other.getId()) return false
        if (volume != other.volume) return false
        if (level != other.level) return false
        if (active != other.active) return false
        if (weight != other.weight) return false
        if (height != other.height) return false
        if (age != other.age) return false
        if (index != other.index) return false
        if (name != other.name) return false
        if (date != other.date) return false

        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(getId(), name, date, volume, level, active, weight, height, age, index)
    }
}

class TestEntity2(
    @Unique1
    val name2: String,
    @Index1
    val category: Long? = null
) : BaseEntity() {

    override fun toString(): String {
        return "TestEntity2(id=${getId()}, name2='$name2', category=$category)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TestEntity2

        if (getId() != other.getId()) return false
        if (category != other.category) return false
        if (name2 != other.name2) return false

        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(getId(), name2, category)
    }
}

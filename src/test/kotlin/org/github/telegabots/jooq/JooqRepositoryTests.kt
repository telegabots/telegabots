package org.github.telegabots.jooq

import org.jooq.impl.DSL
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.sql.DriverManager

@Disabled
class JooqRepositoryTests {
    @Test
    fun testJooqRepo() {
        DriverManager.getConnection("jdbc:sqlite:TestDB.db", "", "").use { c ->
            val result = DSL.using(c)
                .selectFrom(Tables.BLOCKS)
                .fetch()

            assertEquals(0, result.size)
        }
    }
}

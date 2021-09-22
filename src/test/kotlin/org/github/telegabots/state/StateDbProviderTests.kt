package org.github.telegabots.state

import com.google.common.io.Files
import org.github.telegabots.state.sqlite.SqliteStateDbProvider
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertNull

class StateDbProviderTests {
    @Test
    fun testEmptyDb() {
        open("create_test.db") {
            val page = findPageById(0L)

            assertNull(page)
        }
    }

    private fun create(dbFilePath: String): StateDbProvider {
        val target = File(dbFilePath)
        if (target.exists()) {
            target.delete()
        }
        Files.copy(File("TestDB.db"), target)

        return SqliteStateDbProvider.create(dbFilePath)
    }

    private inline fun open(dbFilePath: String, init: StateDbProvider.() -> Unit) {
        create(dbFilePath).apply(init)
        File(dbFilePath).delete()
    }
}

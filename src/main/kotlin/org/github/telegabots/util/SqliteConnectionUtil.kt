package org.github.telegabots.util

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.Location
import org.flywaydb.core.api.configuration.ClassicConfiguration
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager

/**
 * Utility class for SQLite connections
 */
object SqliteConnectionUtil {
    fun getConnection(dbFilePath: String): Connection = getConnection(Paths.get(dbFilePath))

    fun getConnection(dbFilePath: Path): Connection {
        val absolutePath = dbFilePath.toAbsolutePath().toString()
        migrateDb(absolutePath)

        return DriverManager.getConnection("jdbc:sqlite:$absolutePath", "", "").apply {
            this.prepareStatement("PRAGMA foreign_keys = ON;").execute()
        }
    }

    private fun migrateDb(dbFilePath: String) {
        try {
            val config = ClassicConfiguration()
            config.setDataSource("jdbc:sqlite:$dbFilePath", "", "")
            config.setLocations(Location("db/sqlite-migration"))
            val flyway = Flyway(config)
            flyway.migrate()
        } catch (e: Exception) {
            throw IllegalStateException("Migration failed in file $dbFilePath", e)
        }
    }
}

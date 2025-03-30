package org.github.telegabots.api.config

import org.apache.commons.lang3.Validate
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.util.*

/**
 * Bot configuration
 *
 * @param prop properties
 */
open class BotConfig private constructor(prop: Properties) {
    private val props: Properties = Validate.notNull(prop, "props")

    val botName: String
        get() = getProperty("bot.name")

    val botToken: String
        get() = getProperty("bot.token")

    val adminChatId: Long
        get() = getProperty("admin.chatId", "0").toLong()

    val alertChatId: Long
        get() = getProperty("alert.chatId", "0").toLong()

    val stateDbPath: String
        get() = getProperty("state.dbPath", "telegabot.db")

    val notModifiedMessageErrorIgnore: Boolean
        get() = getProperty("error.notModifiedMessage.ignore", "true").toBoolean()

    private fun getProperty(key: String): String {
        return Validate.notNull(props.getProperty(key), "Configuration not found for key: $key")
    }

    private fun getProperty(key: String, defValue: String): String {
        val value = props.getProperty(key)

        return Objects.toString(value, defValue)
    }

    companion object {
        private val log = LoggerFactory.getLogger(BotConfig::class.java)!!

        @JvmStatic
        fun load(fileName: String, throwOnError: Boolean = true): BotConfig {
            val props = Properties()

            try {
                val file = File(fileName).canonicalFile
                FileInputStream(file).use { input -> props.load(input) }
                log.info("Loaded properties from file: {}", file)
            } catch (e: Exception) {
                if (throwOnError) {
                    throw RuntimeException(e)
                }
                log.error("Failed to load properties from file: {}", fileName, e)
            }

            return BotConfig(props)
        }

        fun load(props: Properties) = BotConfig(props)
    }
}

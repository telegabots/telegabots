package org.examples.allfeaturedbot

import org.examples.allfeaturedbot.commands.RootCommand
import org.github.telegabots.api.CommandValidator
import org.github.telegabots.api.ContentType
import org.github.telegabots.api.TelegaBotStarter
import org.github.telegabots.api.config.BotConfig
import org.slf4j.LoggerFactory
import java.util.function.Consumer

class AllFeaturedBot {
    companion object {
        private val log = LoggerFactory.getLogger(AllFeaturedBot::class.java)!!

        @JvmStatic
        fun main(args: Array<String>) {
            log.info("All Featured Bot starting...")

            val config = BotConfig.load("application.properties")
            val starter = TelegaBotStarter(
                config = config,
                serviceProvider = ServiceProviderImpl(),
                rootCommand = RootCommand::class.java
            )

            val validator = starter.getService(CommandValidator::class.java)!!

            validator.validateAll("org.examples.allfeaturedbot.commands")

            starter.start(Consumer { sender ->
                sender.sendMessage(config.adminChatId.toString(), "*All featured bot started*", ContentType.Markdown)
            })
        }
    }
}

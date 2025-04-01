package org.examples.testbot

import org.examples.testbot.commands.RootCommand
import org.github.telegabots.api.*
import org.github.telegabots.api.config.BotConfig
import org.slf4j.LoggerFactory

/**
 * Entry-point of Telegram bot for testing purposes.
 */
class TestBot {
    companion object {
        private val log = LoggerFactory.getLogger(TestBot::class.java)!!

        @JvmStatic
        fun main(args: Array<String>) {
            log.info("TestBot starting...")

            val config = BotConfig.load("application.properties")
            val starter = TelegaBotStarter.builder(RootCommand::class.java)
                .config(config)
                .serviceProvider(ServiceProviderImpl())
                .onStartHandler(TestBot::onStartHandler)
                .build()

            // this is optional, but recommended
            val validator = starter.getService(CommandValidator::class.java)
            validator.validateAll("org.examples.testbot.commands")

            // start the bot
            starter.start()
        }

        private fun onStartHandler(telegaBot: TelegaBot) {
            val config = telegaBot.config
            telegaBot.tryGetService(MessageSender::class.java)!!.sendMessage(
                config.adminChatId.toString(),
                "*Test bot started*",
                ContentType.Markdown
            )
        }
    }
}

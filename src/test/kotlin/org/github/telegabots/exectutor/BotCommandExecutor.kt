package org.github.telegabots.exectutor

import org.github.telegabots.*
import org.github.telegabots.service.JsonService
import org.github.telegabots.state.MemoryStateDbProvider
import org.telegram.telegrambots.meta.api.objects.Update

class BotCommandExecutor(private val rootCommand: Class<out BaseCommand>) {
    private val serviceProvider = mockStrict(ServiceProvider::class.java)
    private val messageSender = mockStrict(MessageSender::class.java)
    private val dbProvider = MemoryStateDbProvider()
    private val jsonService = JsonService()
    private val telegaBot = TelegaBot(messageSender = messageSender,
            serviceProvider = serviceProvider,
            adminChatId = 0,
            dbProvider = dbProvider,
            jsonService = jsonService,
            rootCommand = rootCommand)

    fun handle(update: Update): Boolean {
        return telegaBot.handle(update)
    }
}

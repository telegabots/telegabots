package org.github.telegabots.exectutor

import org.github.telegabots.*
import org.github.telegabots.service.JsonService
import org.github.telegabots.state.MemoryStateDbProvider
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.telegram.telegrambots.meta.api.objects.Update

class BotCommandExecutor(private val rootCommand: Class<out BaseCommand>) {
    private val serviceProvider = mock(ServiceProvider::class.java)
    private val messageSender = mockStrict(MessageSender::class.java)
    private val dbProvider = MemoryStateDbProvider()
    private val jsonService = JsonService()
    private val telegaBot = TelegaBot(
        messageSender = messageSender,
        serviceProvider = serviceProvider,
        adminChatId = 0,
        dbProvider = dbProvider,
        jsonService = jsonService,
        rootCommand = rootCommand
    )

    fun handle(update: Update): Boolean {
        return telegaBot.handle(update)
    }

    fun addService(service: Class<out Service>, instance: Service) {
        Mockito.`when`(serviceProvider.getService(service)).thenReturn(instance)
    }
}

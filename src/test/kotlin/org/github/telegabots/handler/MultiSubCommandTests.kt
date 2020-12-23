package org.github.telegabots.handler

import org.github.telegabots.*
import org.github.telegabots.annotation.CallbackHandler
import org.github.telegabots.annotation.CommandHandler
import org.github.telegabots.test.Page
import org.github.telegabots.test.Page.Companion.page
import org.github.telegabots.test.scenario
import org.junit.jupiter.api.Test

class MultiSubCommandTests : BaseTests() {
    @Test
    fun testMultipleSubcommands() {
        scenario<CommandRoot> {
            assertThat {
                rootNotCalled()
                userMessageNotSentYet()
                blocksCountEmpty()
            }

            user {
                sendTextMessage("/start")
            }

            assertThat {
                userMessageWasSent()
                rootWasCalled()
                notCalled<SubMenu1Command>()
                blocksCount(1)
            }

            val messageId = lastUserMessageId()

            user {
                sendTextMessage("SUB_MENU1")
            }

            assertThat {
                notCalled<SubMenu1Command>()
                rootWasCalled(2)
                blocksCount(1)
            }

            user {
                sendCallbackMessage(messageId = messageId, callbackData = "SUB_MENU1")
            }

            assertThat {
                wasCalled<SubMenu1Command>()
                notCalled<SubMenu2Command>()
                rootWasCalled(2)
                blocksCount(1)
            }

            user {
                sendCallbackMessage(messageId = messageId, callbackData = "SUB_MENU2")
            }

            assertThat {
                wasCalled<SubMenu1Command>()
                wasCalled<SubMenu2Command>()
                rootWasCalled(2)
                blocksCount(1)
            }
        }
    }
}

internal class CommandRoot : BaseCommand() {
    @CommandHandler
    fun handle(msg: String) {
        if (msg == "/start") {
            context.sendMessage(
                "Choose menu:",
                contentType = ContentType.Plain,
                messageType = MessageType.Callback,
                subCommands = listOf(listOf(SubCommand.of<SubMenu1Command>(), SubCommand.of<SubMenu2Command>()))
            )
        }
    }
}

internal class SubMenu1Command : BaseCommand() {
    @CallbackHandler
    fun handle(message: String, messageId: Int) {
    }
}

internal class SubMenu2Command : BaseCommand() {
    @CallbackHandler
    fun handle(message: String, messageId: Int) {
    }
}

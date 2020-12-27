package org.github.telegabots.test

import org.github.telegabots.BaseTests
import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.entity.CommandDef
import org.github.telegabots.api.entity.CommandPage
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

class ScenarioBuilder(private val rootCommand: Class<out BaseCommand>) : BaseTests() {
    private val userId = nextRandomInt()
    private val chatId = nextRandomLong()
    private val executor = createExecutor(rootCommand)
    private val assertBuilder = AssertBuilder()
    private val userBuilder = UserBuilder()
    private var lastHandleResult: Boolean? = null

    fun lastUserMessageId(): Int {
        return executor.lastUserMessageId() ?: throw IllegalStateException("Message not sent yet")
    }

    fun assertThat(action: AssertBuilder.() -> Unit) {
        assertBuilder.apply(action)
    }

    fun user(action: UserBuilder.() -> Unit) {
        userBuilder.apply(action)
    }

    fun resetRootCall() = rootCommand.kotlin.resetCalled()

    inner class AssertBuilder {
        fun rootNotCalled() {
            rootCommand.kotlin.assertNotCalled()
        }

        fun rootWasCalled(expected: Int = 1) {
            rootCommand.kotlin.assertWasCalled(expected)
        }

        fun blocksCountEmpty() {
            val blocksCount = executor.getUserBlocks(userId).size
            assertEquals(0, blocksCount, "Command blocks count expected to be empty")
        }

        fun blocksCount(expected: Int) {
            val blocksCount = executor.getUserBlocks(userId).size
            assertEquals(expected, blocksCount) { "Command blocks count expected to be $blocksCount" }
        }

        fun lastBlockPagesCount(expected: Int) {
            val lastPages = executor.getLastBlockPages(userId).map { Page.from(it) }

            assertEquals(
                expected,
                lastPages.size
            ) { "Pages of last block expected to be $expected, but found ${lastPages.size}. Last pages: $lastPages" }
        }

        fun lastBlockPages(vararg pages: Page) {
            val lastPages = executor.getLastBlockPages(userId).map { Page.from(it) }

            for (idx in 0..Math.min(pages.size, lastPages.size)) {
                assertEquals(pages[idx], lastPages[idx])
            }

            assertEquals(
                pages.size,
                lastPages.size
            ) { "Pages of last command expected to be ${pages.size}, but found ${lastPages.size}. Last pages: $lastPages" }
        }

        fun assertReturnSuccess() {
            assertNotNull(lastHandleResult, "Handle not called")

            assertTrue(lastHandleResult!!, "Last command result is false, expected true")
        }

        fun userMessageNotSentYet() {
            assertNull(executor.lastUserMessageId(), "Last message id expected to be null")
        }

        fun userMessageWasSent(): Int {
            assertNotNull(executor.lastUserMessageId(), "Last message id expected not null")
            return executor.lastUserMessageId()!!
        }

        inline fun <reified T : BaseCommand> notCalled() = CommandAssert.assertNotCalled<T>()

        inline fun <reified T : BaseCommand> wasCalled(expected: Int = 1) =
            CommandAssert.assertWasCalled<T>(expected)
    }

    inner class UserBuilder {
        fun sendTextMessage(messageText: String = "Message sent at " + LocalDateTime.now()): Int? {
            val message = createAnyTextMessage(userId = userId, chatId = chatId, messageText = messageText)
            lastHandleResult = executor.handle(message)
            return executor.lastUserMessageId()
        }

        fun sendInlineMessage(messageId: Int, callbackData: String): Int? {
            val message = createAnyInlineMessage(
                userId = userId,
                chatId = chatId,
                messageId = messageId,
                callbackData = callbackData
            )
            lastHandleResult = executor.handle(message)
            return executor.lastUserMessageId()
        }
    }
}

inline fun <reified T : BaseCommand> scenario(init: ScenarioBuilder.() -> Unit) {
    ScenarioBuilder(T::class.java).apply(init)
}

data class Page(
    val handler: String,
    val commandDefs: List<List<CommandDef>> = emptyList()
) {
    companion object {
        fun from(page: CommandPage): Page = Page(page.handler, page.commandDefs)

        fun page(
            handler: String,
            subCommands: List<List<CommandDef>> = emptyList()
        ): Page = Page(handler, subCommands)
    }
}

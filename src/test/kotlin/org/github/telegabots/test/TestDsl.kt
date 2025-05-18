package org.github.telegabots.test

import org.github.telegabots.BaseTests
import org.github.telegabots.api.BaseController
import org.github.telegabots.api.Service
import org.github.telegabots.entity.ButtonDef
import org.github.telegabots.entity.MessagePage
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

class ScenarioBuilder(private val rootController: Class<out BaseController>, services: List<Service> = emptyList()) :
    BaseTests() {
    private val userId = nextRandomLong()
    private val chatId = nextRandomLong()
    private val executor = createExecutor(rootController, services)
    private val assertBuilder = AssertBuilder()
    private val userBuilder = UserBuilder()
    private var lastHandleResult: Boolean? = null

    init {
        resetAllCalls()
    }

    fun lastUserMessageId(): Int {
        return executor.lastUserMessageId() ?: throw IllegalStateException("Message not sent yet")
    }

    fun assertThat(action: AssertBuilder.() -> Unit) {
        assertBuilder.apply(action)
    }

    fun user(action: UserBuilder.() -> Unit) {
        userBuilder.apply(action)
    }

    fun addService(service: Class<out Service>, instance: Service) {
        executor.addService(service, instance)
    }

    inner class AssertBuilder {
        fun rootNotCalled() {
            rootController.kotlin.assertNotCalled()
        }

        fun rootWasCalled(expected: Int = 1) {
            rootController.kotlin.assertWasCalled(expected)
        }

        fun blocksCountEmpty() {
            val blocksCount = executor.getUserBlocks(userId).size
            assertEquals(0, blocksCount, "Controller blocks count expected to be empty")
        }

        fun blocksCount(expected: Int) {
            val blocksCount = executor.getUserBlocks(userId).size
            assertEquals(expected, blocksCount) { "Controller blocks count expected to be $expected" }
        }

        fun messageBlockPagesCount(messageId: Int, expected: Int) {
            val block = executor.getBlockByMessage(userId, messageId)
            val pages = block?.let { executor.getBlockPages(block.id) } ?: emptyList()

            assertEquals(
                expected,
                pages.size
            ) {
                "Pages of the block (id=${block?.id}) expected to be $expected, but found ${pages.size}. Last pages: " + pages.map {
                    Page.from(
                        it
                    )
                }
            }
        }

        fun lastBlockPagesCount(expected: Int) {
            val lastBlock = executor.getLastBlock(userId)
            val lastPages = lastBlock?.let { executor.getBlockPages(lastBlock.id).map { Page.from(it) } } ?: emptyList()

            assertEquals(
                expected,
                lastPages.size
            ) { "Pages of last block (id=${lastBlock?.id}) expected to be $expected, but found ${lastPages.size}. Last pages: $lastPages" }
        }

        fun lastBlockPages(vararg pages: Page) {
            val lastPages = executor.getLastBlockPages(userId).map { Page.from(it) }

            for (idx in 0..Math.min(pages.size, lastPages.size)) {
                assertEquals(pages[idx], lastPages[idx])
            }

            assertEquals(
                pages.size,
                lastPages.size
            ) { "Pages of last controller expected to be ${pages.size}, but found ${lastPages.size}. Last pages: $lastPages" }
        }

        fun printBlocks() {
            val blocks = executor.getUserBlocks(userId)
            blocks.forEach { block ->
                val pages = executor.getBlockPages(block.id)
                println("Block(id: ${block.id}, pages: ${pages.size})")
                pages.forEach { page ->
                    val buttons = page.buttonDefs.flatten().map { it.titleId }
                    println("  Page(id: ${page.id}, buttonDefs size: ${buttons.size}, buttonDefs: $buttons)")
                }
            }
        }

        fun controllerReturnTrue() {
            assertNotNull(lastHandleResult, "Handle not called")

            assertTrue(lastHandleResult!!, "Last controller result is false, expected true")
        }

        fun controllerReturnFalse() {
            assertNotNull(lastHandleResult, "Handle not called")

            assertFalse(lastHandleResult!!, "Last controller result is true, expected false")
        }

        fun userMessageNotSentYet() {
            assertNull(executor.lastUserMessageId(), "Last message id expected to be null")
        }

        fun userMessageWasSent(): Int {
            assertNotNull(executor.lastUserMessageId(), "Last message id expected not null")
            return executor.lastUserMessageId()!!
        }

        inline fun <reified T : BaseController> notCalled() = ControllerAssert.assertNotCalled<T>()

        inline fun <reified T : BaseController> wasCalled(expected: Int = 1) =
            ControllerAssert.assertWasCalled<T>(expected)
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

        fun addLocalization(vararg localPairs: Pair<String, String>) {
            executor.addLocalization(userId, *localPairs)
        }
    }
}

inline fun <reified T : BaseController> scenario(init: ScenarioBuilder.() -> Unit) {
    ScenarioBuilder(T::class.java).apply(init)
}

inline fun <reified T : BaseController> scenario(services: List<Service>, init: ScenarioBuilder.() -> Unit) {
    ScenarioBuilder(T::class.java, services).apply(init)
}

data class Page(
    val handler: String,
    val buttonDefs: List<List<ButtonDef>> = emptyList()
) {
    companion object {
        fun from(page: MessagePage): Page = Page(page.handler, page.buttonDefs)

        fun page(
            handler: String,
            buttonDefs: List<List<ButtonDef>> = emptyList()
        ): Page = Page(handler, buttonDefs)
    }
}

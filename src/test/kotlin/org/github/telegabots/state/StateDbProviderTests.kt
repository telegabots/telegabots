package org.github.telegabots.state

import com.google.common.io.Files
import org.github.telegabots.api.MessageType
import org.github.telegabots.entity.CommandBlock
import org.github.telegabots.entity.CommandPage
import org.github.telegabots.state.sqlite.SqliteStateDbProvider
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class StateDbProviderTests {
    @Test
    fun testEmptyDb() {
        open {
            if (this is SqliteStateDbProvider) {
                assertEquals(0, getAllBlocks().size)
                assertEquals(0, getAllPages().size)
            }
        }
    }

    @Test
    fun testCreateBlock() {
        open {
            val block =
                saveBlock(CommandBlock(messageId = MESSAGE_ID, userId = USER_ID, messageType = MessageType.Inline))

            assertTrue(block.id > 0)
            assertEquals(USER_ID, block.userId)
            assertEquals(MESSAGE_ID, block.messageId)
            assertEquals(MessageType.Inline, block.messageType)

            val blockFound = findBlockById(block.id)

            assertEquals(block, blockFound)

            val pages = getBlockPages(block.id)

            assertEquals(0, pages.size)

            val blockFoundByMessageId = findBlockByMessageId(USER_ID, MESSAGE_ID)

            assertEquals(block, blockFoundByMessageId)

            val blockFoundByUser = findLastBlockByUserId(USER_ID)

            assertEquals(block, blockFoundByUser)

            if (this is SqliteStateDbProvider) {
                assertEquals(1, getAllBlocks().size)
                assertEquals(0, getAllPages().size)
            }
        }
    }

    @Test
    fun testCreatePage() {
        open {
            val block =
                saveBlock(CommandBlock(messageId = MESSAGE_ID, userId = USER_ID, messageType = MessageType.Inline))
            val page1 = savePage(CommandPage(blockId = block.id, handler = "some_handler"))!!
            val page2 = savePage(CommandPage(blockId = block.id, handler = "another_handler"))!!

            assertTrue(page1.id > 0)
            assertEquals("some_handler", page1.handler)
            assertEquals(0, page1.commandDefs.size)
            assertEquals(block.id, page1.blockId)

            val pageFound = findPageById(page1.id)

            assertEquals(page1, pageFound)

            val pagesFoundByBlock = getBlockPages(block.id)

            assertEquals(2, pagesFoundByBlock.size)
            assertEquals(page1, pagesFoundByBlock.first())
            assertEquals(page2, pagesFoundByBlock.last())

            val lastPage = findLastPageByBlockId(block.id)

            assertEquals(page2, lastPage)

            val blockByPage1Id = findBlockByPageId(page1.id)
            val blockByPage2Id = findBlockByPageId(page2.id)

            assertEquals(block, blockByPage1Id)
            assertEquals(block, blockByPage2Id)

            if (this is SqliteStateDbProvider) {
                assertEquals(1, getAllBlocks().size)
                assertEquals(2, getAllPages().size)
            }
        }
    }

    @Test
    fun testUpdateBlock_ByMessageIdAndUserId() {
        open {
            val block1 =
                saveBlock(CommandBlock(messageId = MESSAGE_ID, userId = USER_ID, messageType = MessageType.Inline))

            val block2 =
                saveBlock(CommandBlock(messageId = MESSAGE_ID, userId = USER_ID, messageType = MessageType.Text))

            assertEquals(block1.id, block2.id)
            assertEquals(USER_ID, block2.userId)
            assertEquals(MESSAGE_ID, block2.messageId)
            assertEquals(MessageType.Text, block2.messageType)

            assertNotEquals(block1, block2)

            if (this is SqliteStateDbProvider) {
                assertEquals(1, getAllBlocks().size)
                assertEquals(0, getAllPages().size)
            }
        }
    }

    @Test
    fun testCreateBlock_IgnoreId() {
        open {
            val block1 =
                saveBlock(CommandBlock(messageId = MESSAGE_ID, userId = USER_ID, messageType = MessageType.Inline))

            val block2 =
                saveBlock(
                    CommandBlock(
                        id = block1.id,
                        messageId = MESSAGE_ID + 1,
                        userId = USER_ID,
                        messageType = MessageType.Inline
                    )
                )

            assertNotEquals(block1.id, block2.id)
            assertEquals(USER_ID, block2.userId)
            assertEquals(MESSAGE_ID + 1, block2.messageId)
            assertEquals(MessageType.Inline, block2.messageType)

            assertNotEquals(block1, block2)

            if (this is SqliteStateDbProvider) {
                assertEquals(2, getAllBlocks().size)
                assertEquals(0, getAllPages().size)
            }
        }
    }

    @Test
    fun testUpdatePage_ById() {
        open {
            val block =
                saveBlock(CommandBlock(messageId = MESSAGE_ID, userId = USER_ID, messageType = MessageType.Inline))
            val page1 = savePage(CommandPage(blockId = block.id, handler = "some_handler"))!!
            val page2 = savePage(CommandPage(id = page1.id, blockId = block.id, handler = "another_handler"))!!

            assertEquals(page1.id, page2.id)
            assertEquals("another_handler", page2.handler)
            assertEquals(block.id, page2.blockId)
            assertEquals(0, page2.commandDefs.size)
            assertNotEquals(page1, page2)

            if (this is SqliteStateDbProvider) {
                assertEquals(1, getAllBlocks().size)
                assertEquals(1, getAllPages().size)
            }
        }
    }

    @Test
    fun testGetLastBlocks() {
        open {
            (0..9).forEach { index ->
                saveBlock(
                    CommandBlock(
                        messageId = MESSAGE_ID + index,
                        userId = USER_ID,
                        messageType = if ((index % 2) == 0) MessageType.Inline else MessageType.Text
                    )
                )
            }

            (0..4).forEach { index ->
                val pages = getLastBlocks(USER_ID, index * 2, 2)

                assertEquals(2, pages.size)
                assertEquals(USER_ID, pages[0].userId)
                assertEquals(USER_ID, pages[1].userId)
                assertEquals(MessageType.Text, pages[0].messageType)
                assertEquals(MessageType.Inline, pages[1].messageType)
                assertEquals(MESSAGE_ID + 9 - 2 * index, pages[0].messageId)
                assertEquals(MESSAGE_ID + 8 - 2 * index, pages[1].messageId)
            }

            val allBlocks = getLastBlocks(USER_ID, 0, 100)

            assertEquals(10, allBlocks.size)

            val emptyBlocks = getLastBlocks(USER_ID, 10, 1)

            assertEquals(0, emptyBlocks.size)
            assertEquals(10, getBlocksCount(USER_ID))

            if (this is SqliteStateDbProvider) {
                assertEquals(10, getAllBlocks().size)
                assertEquals(0, getAllPages().size)
            }
        }
    }

    @Test
    fun testCreateBlock_Failed_When_MessageId_IsInvalid() {
        open {
            val ex = assertThrows<IllegalStateException> {
                saveBlock(
                    CommandBlock(
                        messageId = 0,
                        userId = USER_ID,
                        messageType = MessageType.Inline,
                        createdAt = NOW
                    )
                )
            }

            assertEquals(
                "Block is invalid: CommandBlock(messageId=0, userId=100, messageType=Inline, id=0, createdAt=2021-09-23T23:23:13.467)",
                ex.message
            )

            if (this is SqliteStateDbProvider) {
                assertEquals(0, getAllBlocks().size)
                assertEquals(0, getAllPages().size)
            }
        }
    }

    @Test
    fun testCreateBlock_Failed_When_UserId_IsInvalid() {
        open {
            val ex = assertThrows<IllegalStateException> {
                saveBlock(
                    CommandBlock(
                        messageId = MESSAGE_ID,
                        userId = 0,
                        messageType = MessageType.Inline,
                        createdAt = NOW
                    )
                )
            }

            assertEquals(
                "Block is invalid: CommandBlock(messageId=10000, userId=0, messageType=Inline, id=0, createdAt=2021-09-23T23:23:13.467)",
                ex.message
            )

            if (this is SqliteStateDbProvider) {
                assertEquals(0, getAllBlocks().size)
                assertEquals(0, getAllPages().size)
            }
        }
    }

    @Test
    fun testCreatePage_Failed_When_Handler_IsInvalid() {
        open {
            val block =
                saveBlock(CommandBlock(messageId = MESSAGE_ID, userId = USER_ID, messageType = MessageType.Inline))

            val ex = assertThrows<IllegalStateException> {
                savePage(CommandPage(blockId = block.id, handler = " ", createdAt = NOW, updatedAt = NOW))!!
            }

            assertEquals(
                "Page is invalid: CommandPage(id=0, blockId=1, handler= , commandDefs=[], createdAt=2021-09-23T23:23:13.467, updatedAt=2021-09-23T23:23:13.467)",
                ex.message
            )

            val ex2 = assertThrows<IllegalStateException> {
                savePage(CommandPage(blockId = block.id, handler = "", createdAt = NOW, updatedAt = NOW))!!
            }

            assertEquals(
                "Page is invalid: CommandPage(id=0, blockId=1, handler=, commandDefs=[], createdAt=2021-09-23T23:23:13.467, updatedAt=2021-09-23T23:23:13.467)",
                ex2.message
            )

            if (this is SqliteStateDbProvider) {
                assertEquals(1, getAllBlocks().size)
                assertEquals(0, getAllPages().size)
            }
        }
    }

    @Test
    fun testCreatePage_Failed_When_BlockId_IsInvalid() {
        open {
            val ex = assertThrows<IllegalStateException> {
                savePage(CommandPage(blockId = 0, handler = "foo_bar", createdAt = NOW, updatedAt = NOW))!!
            }

            assertEquals(
                "Page is invalid: CommandPage(id=0, blockId=0, handler=foo_bar, commandDefs=[], createdAt=2021-09-23T23:23:13.467, updatedAt=2021-09-23T23:23:13.467)",
                ex.message
            )

            val ex2 = assertThrows<IllegalStateException> {
                savePage(CommandPage(blockId = -100, handler = "foo_bar2", createdAt = NOW, updatedAt = NOW))!!
            }

            assertEquals(
                "Page is invalid: CommandPage(id=0, blockId=-100, handler=foo_bar2, commandDefs=[], createdAt=2021-09-23T23:23:13.467, updatedAt=2021-09-23T23:23:13.467)",
                ex2.message
            )

            if (this is SqliteStateDbProvider) {
                assertEquals(0, getAllBlocks().size)
                assertEquals(0, getAllPages().size)
            }
        }
    }

    private inline fun open(dbFilePath: String = "test.db", init: StateDbProvider.() -> Unit) {
        val target = File(dbFilePath)
        if (target.exists()) {
            target.delete()
        }
        try {
            create(target).apply(init)
        } finally {
            target.delete()
        }
    }

    private fun create(target: File): StateDbProvider {
        Files.copy(File("TestDB.db"), target)

        return SqliteStateDbProvider.create(target.absolutePath)
    }

    private companion object {
        const val USER_ID: Long = 100
        const val MESSAGE_ID: Int = 10_000
        val NOW: LocalDateTime = LocalDateTime.parse("2021-09-23T23:23:13.467")
    }
}

package org.github.telegabots.state

import org.github.telegabots.api.*
import org.github.telegabots.entity.ButtonDef
import org.github.telegabots.entity.MessageBlock
import org.github.telegabots.entity.MessagePage
import org.github.telegabots.entity.StateDef
import org.github.telegabots.service.InternalJsonService
import org.github.telegabots.util.Validation
import org.github.telegabots.util.runIn
import java.util.concurrent.locks.Lock

/**
 * Stores all user-related states
 */
internal class UserStateService(
    private val userId: Long,
    private val dbProvider: LockableStateDbProvider,
    private val jsonService: InternalJsonService,
    private val globalState: StateProvider,
    private val serviceProvider: ServiceProvider
) : UserService {
    private val sharedStates: MutableMap<Int, StateProvider> = mutableMapOf()
    private val localStates: MutableMap<Long, StateProvider> = mutableMapOf()
    private val userState = serviceProvider.getUserService(UserStateProvider::class.java, userId)
    private val localizationProvider = serviceProvider.getUserService(UserLocalizationProvider::class.java, userId)

    override fun userId(): Long = userId

    fun getReadLock(): Lock = dbProvider.readLock()

    fun getWriteLock(): Lock = dbProvider.writeLock()

    fun getBlockByMessageId(messageId: Int): MessageBlock? = dbProvider.findBlockByMessageId(userId, messageId)

    fun findBlockById(blockId: Long): MessageBlock? = dbProvider.findBlockById(blockId)

    fun getLastBlock(): MessageBlock? = dbProvider.findLastBlockByUserId(userId)

    fun getLastBlocks(lastIndexFrom: Int, pageSize: Int): List<MessageBlock> =
        dbProvider.getLastBlocks(userId, lastIndexFrom, pageSize)

    fun findLastPage(blockId: Long): MessagePage? = dbProvider.findLastPageByBlockId(blockId)

    fun getLastPage(blockId: Long): MessagePage =
        findLastPage(blockId) ?: throw IllegalStateException("Page not found by blockId: $blockId")

    fun getPages(blockId: Long): List<MessagePage> = dbProvider.getBlockPages(blockId)

    fun removePage(pageId: Long): MessagePage? = dbProvider.deletePage(pageId)

    fun findPageById(pageId: Long): MessagePage? = dbProvider.findPageById(pageId)

    fun pageExists(pageId: Long): Boolean = findPageById(pageId) != null

    fun blockExists(blockId: Long): Boolean = findBlockById(blockId) != null

    fun saveBlock(messageId: Int, messageType: MessageType): MessageBlock =
        dbProvider.saveBlock(
            MessageBlock(
                messageId = messageId,
                userId = userId,
                messageType = messageType,
                id = 0
            )
        )

    fun savePage(
        blockId: Long,
        handler: Class<out BaseController>,
        buttons: List<List<Button>> = emptyList(),
        pageId: Long = 0
    ): MessagePage? =
        dbProvider.savePage(
            MessagePage(
                id = pageId,
                blockId = blockId,
                handler = handler.name,
                buttonDefs = toButtonDefs(buttons)
            )
        )

    fun getStates(messageId: Int, pageId: Long): States =
        StatesImpl(
            localState = getLocalStateInternal(pageId),
            sharedState = getSharedState(messageId),
            userState = userState,
            globalState = globalState
        )

    fun getStates(messageId: Int, state: StateDef?, pageId: Long = 0): States =
        StatesImpl(
            localState = if (pageId > 0) getLocalStateInternal(pageId, state) else LocalTempStateProvider(
                state,
                jsonService
            ),
            sharedState = getSharedState(messageId),
            userState = userState,
            globalState = globalState
        )

    fun getStates(): States =
        StatesImpl(
            localState = LocalTempStateProvider(StateDef.Empty, jsonService),
            sharedState = LocalTempStateProvider(StateDef.Empty, jsonService),
            userState = userState,
            globalState = globalState
        )

    fun getLocalStateProvider(pageId: Long): StateProvider {
        return synchronized(localStates) {
            localStates.getOrPut(pageId) { LocalStateProvider(pageId, dbProvider, jsonService) }
        }
    }

    fun getSharedStateProvider(messageId: Int): StateProvider = getSharedState(messageId)

    /**
     * Flushes dirty states to db
     */
    fun flush() {
        TODO()
    }

    /**
     * Clones specified block and returns last page from cloned block
     */
    fun cloneFromBlock(blockId: Long, newMessageId: Int): MessagePage? {
        Validation.validateMessageId(newMessageId)

        dbProvider.writeLock().runIn {
            val block = findBlockById(blockId)

            if (block != null) {
                val pages = getPages(blockId)
                val sharedState = dbProvider.getSharedState(userId, block.messageId)
                val localStates = dbProvider.getLocalStates(blockId)

                val newBlock = dbProvider.saveBlock(
                    MessageBlock(
                        messageId = newMessageId,
                        userId = userId,
                        messageType = block.messageType
                    )
                )

                dbProvider.saveSharedState(userId, newMessageId, sharedState)

                val newPages = pages.map { page ->
                    val newPage = dbProvider.savePage(page.copy(blockId = newBlock.id, id = 0))!!
                    val state = localStates[page.id]

                    if (state != null) {
                        dbProvider.saveLocalState(newPage.id, state)
                    }

                    newPage
                }

                return newPages.last()
            }
        }

        return null
    }

    fun findBlockByPageId(pageId: Long): MessageBlock? = dbProvider.findBlockByPageId(pageId)

    fun findBlockByMessageId(messageId: Int): MessageBlock? = dbProvider.findBlockByMessageId(userId, messageId)

    private fun getSharedState(messageId: Int): StateProvider {
        return synchronized(sharedStates) {
            sharedStates.getOrPut(messageId) { SharedStateProvider(userId, messageId, dbProvider, jsonService) }
        }
    }

    private fun getLocalStateInternal(pageId: Long, state: StateDef? = null): StateProvider =
        if (state != null)
            AdditionalStateProvider(getLocalStateProvider(pageId), state, jsonService)
        else
            getLocalStateProvider(pageId)

    private fun toButtonDefs(buttons: List<List<Button>>): List<List<ButtonDef>> =
        buttons.map { it.map { cmd -> toButtonDef(cmd) } }

    private fun toButtonDef(cmd: Button): ButtonDef {
        return ButtonDef(
            titleId = cmd.titleId,
            title = cmd.title ?: localizationProvider.getString(cmd.titleId),
            handler = cmd.handler?.name,
            state = jsonService.toStateDef(cmd.state)
        )
    }

    fun deleteBlock(blockId: Long) {
        dbProvider.deleteBlock(blockId)
    }

    fun deletePage(pageId: Long) {
        dbProvider.deletePage(pageId)
    }
}

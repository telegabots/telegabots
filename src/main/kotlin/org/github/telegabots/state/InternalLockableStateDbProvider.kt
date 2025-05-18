package org.github.telegabots.state

import org.github.telegabots.entity.MessageBlock
import org.github.telegabots.entity.MessagePage
import org.github.telegabots.entity.StateDef
import org.github.telegabots.util.runIn
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock

/**
 * Wrapper over [StateDbProvider] to support implementation of [LockableStateDbProvider]
 */
internal class InternalLockableStateDbProvider(
    private val delegate: StateDbProvider,
    readWriteLock: ReadWriteLock
) : LockableStateDbProvider {
    private val readLock = readWriteLock.readLock()
    private val writeLock = readWriteLock.writeLock()

    override fun readLock(): Lock = readLock

    override fun writeLock(): Lock = writeLock

    override fun saveBlock(block: MessageBlock): MessageBlock {
        writeLock.runIn {
            return delegate.saveBlock(block)
        }
    }

    override fun savePage(page: MessagePage): MessagePage? {
        writeLock.runIn {
            return delegate.savePage(page)
        }
    }


    override fun findPageById(pageId: Long): MessagePage? {
        readLock.runIn {
            return delegate.findPageById(pageId)
        }
    }

    override fun findBlockById(blockId: Long): MessageBlock? {
        readLock.runIn {
            return delegate.findBlockById(blockId)
        }
    }

    override fun findBlockByMessageId(userId: Long, messageId: Int): MessageBlock? {
        readLock.runIn {
            return delegate.findBlockByMessageId(userId, messageId)
        }
    }

    override fun findBlockIdByMessageId(userId: Long, messageId: Int): Long? {
        readLock.runIn {
            return delegate.findBlockIdByMessageId(userId, messageId)
        }
    }

    override fun findLastBlockByUserId(userId: Long): MessageBlock? {
        readLock.runIn {
            return delegate.findLastBlockByUserId(userId)
        }
    }

    override fun findLastPageByBlockId(blockId: Long): MessagePage? {
        readLock.runIn {
            return delegate.findLastPageByBlockId(blockId)
        }
    }

    override fun findBlockByPageId(pageId: Long): MessageBlock? {
        readLock.runIn {
            return delegate.findBlockByPageId(pageId)
        }
    }

    override fun getBlockPages(blockId: Long): List<MessagePage> {
        readLock.runIn {
            return delegate.getBlockPages(blockId)
        }
    }

    override fun getBlocksCount(userId: Long): Int {
        readLock.runIn {
            return delegate.getBlocksCount(userId)
        }
    }

    override fun getLastBlocks(userId: Long, lastIndexFrom: Int, pageSize: Int): List<MessageBlock> {
        readLock.runIn {
            return delegate.getLastBlocks(userId, lastIndexFrom, pageSize)
        }
    }

    override fun saveLocalState(pageId: Long, state: StateDef) {
        writeLock.runIn {
            return delegate.saveLocalState(pageId, state)
        }
    }

    override fun findLocalState(pageId: Long): StateDef? {
        readLock.runIn {
            return delegate.findLocalState(pageId)
        }
    }

    override fun getLocalStates(blockId: Long): Map<Long, StateDef> {
        readLock.runIn {
            return delegate.getLocalStates(blockId)
        }
    }

    override fun saveSharedState(userId: Long, messageId: Int, state: StateDef) {
        writeLock.runIn {
            return delegate.saveSharedState(userId, messageId, state)
        }
    }

    override fun findSharedState(userId: Long, messageId: Int): StateDef? {
        readLock.runIn {
            return delegate.findSharedState(userId, messageId)
        }
    }

    override fun findUserState(userId: Long): StateDef? {
        readLock.runIn {
            return delegate.findUserState(userId)
        }
    }

    override fun saveUserState(userId: Long, state: StateDef) {
        writeLock.runIn {
            return delegate.saveUserState(userId, state)
        }
    }

    override fun findGlobalState(): StateDef? {
        readLock.runIn {
            return delegate.findGlobalState()
        }
    }

    override fun saveGlobalState(state: StateDef) {
        writeLock.runIn {
            return delegate.saveGlobalState(state)
        }
    }

    override fun deleteBlock(blockId: Long): MessageBlock? {
        writeLock.runIn {
            return delegate.deleteBlock(blockId)
        }
    }

    override fun deletePage(pageId: Long): MessagePage? {
        writeLock.runIn {
            return delegate.deletePage(pageId)
        }
    }
}

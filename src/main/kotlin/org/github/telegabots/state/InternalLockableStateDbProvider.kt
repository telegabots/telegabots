package org.github.telegabots.state

import org.github.telegabots.entity.CommandBlock
import org.github.telegabots.entity.CommandPage
import org.github.telegabots.entity.StateDef
import org.github.telegabots.util.runIn
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Wrapper over StateDbProvider to support implementation of LockableStateDbProvider
 */
class InternalLockableStateDbProvider(private val delegate: StateDbProvider) : LockableStateDbProvider {
    private val rwl: ReadWriteLock = ReentrantReadWriteLock()
    private val readLock = rwl.readLock()
    private val writeLock = rwl.writeLock()

    override fun readLock(): Lock = readLock

    override fun writeLock(): Lock = writeLock

    override fun saveBlock(block: CommandBlock): CommandBlock {
        writeLock.runIn {
            return delegate.saveBlock(block)
        }
    }

    override fun savePage(page: CommandPage): CommandPage? {
        writeLock.runIn {
            return delegate.savePage(page)
        }
    }

    override fun removePage(pageId: Long): CommandPage? {
        writeLock.runIn {
            return delegate.removePage(pageId)
        }
    }

    override fun findPageById(pageId: Long): CommandPage? {
        readLock.runIn {
            return delegate.findPageById(pageId)
        }
    }

    override fun findBlockById(blockId: Long): CommandBlock? {
        readLock.runIn {
            return delegate.findBlockById(blockId)
        }
    }

    override fun findBlockByMessageId(userId: Int, messageId: Int): CommandBlock? {
        readLock.runIn {
            return delegate.findBlockByMessageId(userId, messageId)
        }
    }

    override fun findLastBlockByUserId(userId: Int): CommandBlock? {
        readLock.runIn {
            return delegate.findLastBlockByUserId(userId)
        }
    }

    override fun findLastPageByBlockId(blockId: Long): CommandPage? {
        readLock.runIn {
            return delegate.findLastPageByBlockId(blockId)
        }
    }

    override fun findBlockByPageId(pageId: Long): CommandBlock? {
        readLock.runIn {
            return delegate.findBlockByPageId(pageId)
        }
    }

    override fun getBlockPages(blockId: Long): List<CommandPage> {
        readLock.runIn {
            return delegate.getBlockPages(blockId)
        }
    }

    override fun getLastBlocks(userId: Int, lastIndexFrom: Int, pageSize: Int): List<CommandBlock> {
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

    override fun saveSharedState(userId: Int, messageId: Int, state: StateDef) {
        writeLock.runIn {
            return delegate.saveSharedState(userId, messageId, state)
        }
    }

    override fun findSharedState(userId: Int, messageId: Int): StateDef? {
        readLock.runIn {
            return delegate.findSharedState(userId, messageId)
        }
    }

    override fun findUserState(userId: Int): StateDef? {
        readLock.runIn {
            return delegate.findUserState(userId)
        }
    }

    override fun saveUserState(userId: Int, state: StateDef) {
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

    override fun deleteBlock(blockId: Long) {
        writeLock.runIn {
            return delegate.deleteBlock(blockId)
        }
    }

    override fun deletePage(pageId: Long) {
        writeLock.runIn {
            return delegate.deletePage(pageId)
        }
    }
}

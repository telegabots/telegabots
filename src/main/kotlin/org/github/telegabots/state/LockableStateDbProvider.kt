package org.github.telegabots.state

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock

/**
 * Thread-safe version of [StateDbProvider]
 */
interface LockableStateDbProvider : StateDbProvider {
    fun readLock(): Lock

    fun writeLock(): Lock

    companion object {
        /**
         * Creates [LockableStateDbProvider] from [StateDbProvider]
         */
        @JvmStatic
        fun of(stateDbProvider: StateDbProvider, readWriteLock: ReadWriteLock): LockableStateDbProvider =
            if (stateDbProvider is LockableStateDbProvider) {
                stateDbProvider
            } else {
                InternalLockableStateDbProvider(stateDbProvider, readWriteLock)
            }
    }
}
